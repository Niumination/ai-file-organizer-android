package com.arena.aifileorganizer.organizer

import com.arena.aifileorganizer.data.GeminiClient
import com.arena.aifileorganizer.model.AiFileDecision
import com.arena.aifileorganizer.model.CategoryPresets
import com.arena.aifileorganizer.model.FileContent
import com.arena.aifileorganizer.model.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AiCategorizer(private val gemini: GeminiClient) {
    suspend fun categorizeBatch(files: List<Pair<ScannedFile, FileContent>>): Map<String, AiFileDecision> = withContext(Dispatchers.Default) {
        val result = mutableMapOf<String, AiFileDecision>()
        files.chunked(12).forEach { chunk -> result.putAll(categorizeChunk(chunk)) }
        result
    }
    private suspend fun categorizeChunk(chunk: List<Pair<ScannedFile, FileContent>>): Map<String, AiFileDecision> {
        val prompt = buildString {
            appendLine("Kamu AI file organizer Indonesia. Klasifikasikan file.")
            appendLine("KATEGORI WAJIB: ${CategoryPresets.allowed.joinToString(", ")}")
            appendLine("Sub kategori: ${CategoryPresets.subFolders}")
            appendLine("Buat nama file RAPI: YYYY-MM-DD_JudulSingkat.Ekstensi")
            appendLine("JANGAN karakter ilegal: \\ / : * ? \" < > |")
            appendLine("""Balas HANYA JSON array: [{"id":"0","category":"Dokumen_Kerja","sub":"PDF","newName":"2024-06-12_Laporan.pdf","confidence":0.92,"reason":"..."}]""")
            appendLine()
            chunk.forEachIndexed { idx, (file, content) ->
                appendLine("--- FILE id=$idx ---")
                appendLine("nama: ${file.displayName}"); appendLine("mime: ${file.mimeType}")
                appendLine("size: ${file.size}"); appendLine("modified: ${file.lastModified}")
                val preview = (content.ocrText ?: "") + "\n" + content.textPreview
                appendLine("isi_preview: ${preview.take(600)}"); appendLine()
            }
        }
        val resp = gemini.generate(prompt) ?: return chunk.associate { it.first.uri.toString() to fallback(it.first) }
        val jsonClean = resp.substringAfter("[", "[").substringBeforeLast("]") + "]"
        return try {
            val clean = if (resp.trim().startsWith("[")) resp else "[$jsonClean"
            val arr = JSONArray(if (clean.contains("[")) clean else "[]")
            val map = mutableMapOf<String, AiFileDecision>()
            for (i in 0 until minOf(arr.length(), chunk.size)) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id", "$i").toIntOrNull() ?: i
                if (id >= chunk.size) continue
                val file = chunk[id].first
                val catRaw = o.optString("category", "Lainnya")
                val category = CategoryPresets.allowed.find { it.equals(catRaw, true) } ?: "Lainnya"
                val sub = o.optString("sub").takeIf { it.isNotBlank() && it != "null" }
                var newName = o.optString("newName", file.displayName).replace(Regex("[\\\\/:*?\"<>|]"), "_")
                if (!newName.contains(".")) {
                    val ext = file.displayName.substringAfterLast('.', "")
                    if (ext.isNotEmpty()) newName += ".$ext"
                }
                map[file.uri.toString()] = AiFileDecision(category, sub, newName.take(120), o.optDouble("confidence", 0.7).toFloat(), o.optString("reason", "-"))
            }
            chunk.forEach { (file, _) -> if (!map.containsKey(file.uri.toString())) map[file.uri.toString()] = fallback(file) }
            map
        } catch (e: Exception) { chunk.associate { it.first.uri.toString() to fallback(it.first) } }
    }
    private fun fallback(file: ScannedFile): AiFileDecision {
        val ext = file.displayName.substringAfterLast('.', "").lowercase()
        val cat = when (ext) {
            "jpg","jpeg","png","webp","heic" -> "Foto_Pribadi"
            "mp4","mkv","mov" -> "Video"
            "mp3","m4a","wav","flac" -> "Audio_Musik"
            "pdf","doc","docx","xls","xlsx","ppt","pptx" -> "Dokumen_Kerja"
            "apk" -> "APK_Installer"
            else -> "Download_Random"
        }
        return AiFileDecision(cat, null, file.displayName, 0.4f, "fallback")
    }
}
