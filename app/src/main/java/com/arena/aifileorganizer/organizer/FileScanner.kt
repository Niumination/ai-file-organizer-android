package com.arena.aifileorganizer.organizer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.arena.aifileorganizer.model.ScannedFile

class FileScanner(private val context: Context) {
    private val forbiddenNames = setOf("Android", "android", ".android_secure", "MIUI", "LOST.DIR", "obb", "data")
    private fun isSafeDocument(doc: DocumentFile, rootPath: String): Boolean {
        val name = doc.name ?: return false
        if (forbiddenNames.any { name.equals(it, true) }) return false
        val fullPath = "$rootPath/$name"
        if (fullPath.contains("/Android/", true)) return false
        return true
    }
    fun scanTree(treeUri: Uri, maxFiles: Int = 600, maxDepth: Int = 5): List<ScannedFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val out = mutableListOf<ScannedFile>()
        fun walk(dir: DocumentFile, rel: String, depth: Int) {
            if (out.size >= maxFiles || depth > maxDepth) return
            val children = try { dir.listFiles() } catch (_: Exception) { emptyArray() }
            for (f in children) {
                if (!isSafeDocument(f, rel)) continue
                if (f.isDirectory) {
                    walk(f, if (rel.isEmpty()) f.name!! else "$rel/${f.name}", depth + 1)
                } else if (f.isFile && f.canRead()) {
                    val name = f.name ?: continue
                    if (name.startsWith(".")) continue
                    out.add(ScannedFile(f.uri, name, f.type, f.length(), f.lastModified(), rel))
                    if (out.size >= maxFiles) return
                }
            }
        }
        walk(root, "", 0)
        return out
    }
}
