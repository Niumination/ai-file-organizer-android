package com.arena.aifileorganizer.organizer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.arena.aifileorganizer.model.ScannedFile

/**
 * Recursive SAF scanner with a safety blacklist so system folders are never
 * touched — including the app's own `AI_Organized` output (prevents
 * re-organizing already organized files on the next run).
 */
class FileScanner(private val context: Context) {

    companion object {
        const val OUTPUT_FOLDER = "AI_Organized"
    }

    /**
     * Exact folder/file names that must never be indexed. Note the generic
     * "/Android/" path check in [isSafeDocument] already covers Android/data
     * and Android/obb for any storage location.
     */
    private val forbiddenNames = setOf(
        "Android", "android", ".android_secure", "MIUI", "LOST.DIR",
        OUTPUT_FOLDER
    )

    private fun isSafeDocument(doc: DocumentFile, rootPath: String): Boolean {
        val name = doc.name ?: return false
        if (forbiddenNames.any { name.equals(it, ignoreCase = true) }) return false
        // Hidden system/junk dirs like .thumbnails, .Trash etc.
        if (name.startsWith(".")) return false
        val fullPath = "$rootPath/$name"
        if (fullPath.contains("/Android/", ignoreCase = true)) return false
        return true
    }

    fun scanTree(treeUri: Uri, maxFiles: Int = 600, maxDepth: Int = 5): List<ScannedFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val out = ArrayList<ScannedFile>(maxFiles.coerceAtMost(600))
        fun walk(dir: DocumentFile, rel: String, depth: Int) {
            if (out.size >= maxFiles || depth > maxDepth) return
            val children = try { dir.listFiles() } catch (_: Exception) { emptyArray() }
            for (f in children) {
                if (out.size >= maxFiles) return
                if (!isSafeDocument(f, rel)) continue
                if (f.isDirectory) {
                    val name = f.name ?: continue
                    walk(f, if (rel.isEmpty()) name else "$rel/$name", depth + 1)
                } else if (f.isFile && f.canRead()) {
                    val name = f.name ?: continue
                    out.add(ScannedFile(f.uri, name, f.type, f.length(), f.lastModified(), rel))
                }
            }
        }
        walk(root, "", 0)
        return out
    }
}
