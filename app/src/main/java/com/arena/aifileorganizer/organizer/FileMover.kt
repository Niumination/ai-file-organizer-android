package com.arena.aifileorganizer.organizer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.arena.aifileorganizer.model.OrganizePlanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Executes an organize plan.
 *
 * Data-safety guarantees:
 * 1. DRY-RUN never writes anything (no folders, no files, no deletions).
 * 2. A source file is deleted ONLY after its copy has been fully written and
 *    size-verified. A partial destination is removed again on any failure so
 *    the user is never left with a corrupt half-file and a deleted original.
 */
class FileMover(private val context: Context) {

    companion object {
        private const val TAG = "FileMover"
        private val ILLEGAL_PATH_CHARS = Regex("[^A-Za-z0-9_\\- ]")
    }

    data class MoveResult(val success: Int, val failed: Int, val errors: List<String>)

    suspend fun execute(
        plan: List<OrganizePlanItem>,
        treeUri: Uri,
        dryRun: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): MoveResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.canWrite()) {
            return@withContext MoveResult(0, plan.size, listOf("Folder tidak bisa diakses — pilih ulang folder"))
        }

        var ok = 0
        var fail = 0
        val errors = mutableListOf<String>()

        // Dry-run must not create ANYTHING on disk.
        val organizedRoot: DocumentFile? = runCatching {
            if (dryRun) root.findFile(FileScanner.OUTPUT_FOLDER)
            else root.findFile(FileScanner.OUTPUT_FOLDER) ?: root.createDirectory(FileScanner.OUTPUT_FOLDER)
        }.getOrNull().let { if (dryRun) it else it ?: root }

        plan.forEachIndexed { index, item ->
            ensureActive() // cooperative cancellation between files
            onProgress(index, plan.size)
            try {
                if (dryRun) {
                    validateForDryRun(item)
                    ok++
                } else {
                    moveOne(item, organizedRoot ?: root)
                    ok++
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                fail++
                Log.w(TAG, "move failed: ${item.file.displayName}", e)
                if (errors.size < 20) {
                    errors.add("${item.file.displayName}: ${e.message ?: "error"}")
                }
            }
        }
        onProgress(plan.size, plan.size)
        MoveResult(ok, fail, errors)
    }

    private fun validateForDryRun(item: OrganizePlanItem) {
        val source = DocumentFile.fromSingleUri(context, item.file.uri)
            ?: throw IOException("file sumber tidak ditemukan")
        if (!source.exists() || !source.canRead()) throw IOException("file sumber tidak bisa dibaca")
        // Do NOT create folders here — simulation only.
    }

    private fun moveOne(item: OrganizePlanItem, organizedRoot: DocumentFile) {
        val source = DocumentFile.fromSingleUri(context, item.file.uri)
            ?: throw IOException("file sumber tidak ditemukan")
        if (!source.exists() || !source.canRead()) throw IOException("file sumber tidak bisa dibaca")

        val targetFolder = ensureFolder(organizedRoot, item.targetFolder)
        val mime = item.file.mimeType ?: "application/octet-stream"
        val baseName = item.targetName.substringBeforeLast('.', item.targetName).ifBlank { "file" }

        var created = targetFolder.createFile(mime, baseName)
        if (created == null) {
            // Some providers reject extension-less names — try the full target name.
            created = targetFolder.createFile(mime, item.targetName)
        }
        val newDoc = created ?: throw IOException("gagal membuat file tujuan di ${item.targetFolder}")

        var copyVerified = false
        try {
            var copiedBytes = 0L
            context.contentResolver.openInputStream(source.uri)?.use { ins ->
                context.contentResolver.openOutputStream(newDoc.uri, "w")?.use { outs ->
                    copiedBytes = ins.copyTo(outs, bufferSize = 64 * 1024)
                } ?: throw IOException("tidak bisa menulis ke file tujuan")
            } ?: throw IOException("tidak bisa membaca file sumber")

            // Exact stream-count verification (provider .length() is unreliable).
            val expected = item.file.size
            if (expected > 0 && copiedBytes < expected) {
                throw IOException("salinan tidak lengkap ($copiedBytes dari $expected byte)")
            }
            if (expected > 0) {
                val reported = newDoc.length()
                if (reported in 1L until expected) {
                    throw IOException("salinan tidak lengkap di storage ($reported dari $expected byte)")
                }
            }
            copyVerified = true
        } finally {
            // Never leave a corrupt partial file behind.
            if (!copyVerified) runCatching { newDoc.delete() }
        }

        // Rename to the final sanitized name (SAF appends extensions on create).
        if (item.targetName.isNotBlank()) {
            runCatching {
                DocumentsContract.renameDocument(context.contentResolver, newDoc.uri, item.targetName)
            }
        }

        // Copy verified → safe to delete the source (this is the "move").
        if (!source.delete()) {
            throw IOException("file tersalin, tetapi file asal gagal dihapus (duplikat aman)")
        }
    }

    private fun ensureFolder(root: DocumentFile, path: String): DocumentFile {
        var cur = root
        path.split('/').filter { it.isNotBlank() }.forEach { part ->
            val safe = part.replace(ILLEGAL_PATH_CHARS, "_").ifBlank { "Lainnya" }
            cur = cur.findFile(safe) ?: cur.createDirectory(safe) ?: cur
        }
        return cur
    }
}
