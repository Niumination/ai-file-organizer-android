package com.arena.aifileorganizer.organizer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.arena.aifileorganizer.model.OrganizePlanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileMover(private val context: Context) {
    data class MoveResult(val success: Int, val failed: Int, val errors: List<String>)
    suspend fun execute(plan: List<OrganizePlanItem>, treeUri: Uri, dryRun: Boolean = false): MoveResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext MoveResult(0, plan.size, listOf("Invalid treeUri"))
        var ok = 0; var fail = 0; val errors = mutableListOf<String>()
        val organizedRoot = root.findFile("AI_Organized") ?: root.createDirectory("AI_Organized") ?: root
        for (item in plan) {
            try {
                val source = DocumentFile.fromSingleUri(context, item.file.uri) ?: continue
                val targetFolder = ensureFolder(organizedRoot, item.targetFolder)
                if (dryRun) { ok++; continue }
                val mime = item.file.mimeType ?: "application/octet-stream"
                val newDoc = targetFolder.createFile(mime, item.targetName.removeSuffix("." + item.targetName.substringAfterLast('.', ""))) ?: targetFolder.createFile(mime, item.targetName)
                if (newDoc == null) { fail++; errors.add("Create failed ${item.targetName}"); continue }
                context.contentResolver.openInputStream(source.uri)?.use { ins ->
                    context.contentResolver.openOutputStream(newDoc.uri)?.use { outs -> ins.copyTo(outs) }
                }
                try { DocumentsContract.renameDocument(context.contentResolver, newDoc.uri, item.targetName) } catch (_: Exception) {}
                source.delete(); ok++
            } catch (e: Exception) { fail++; errors.add("${item.file.displayName}: ${e.message}") }
        }
        MoveResult(ok, fail, errors)
    }
    private fun ensureFolder(root: DocumentFile, path: String): DocumentFile {
        var cur = root
        path.split('/').filter { it.isNotBlank() }.forEach { part ->
            val safe = part.replace(Regex("[^A-Za-z0-9_\\- ]"), "_")
            cur = cur.findFile(safe) ?: cur.createDirectory(safe) ?: cur
        }
        return cur
    }
}
