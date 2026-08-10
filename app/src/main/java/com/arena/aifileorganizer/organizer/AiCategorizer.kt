package com.arena.aifileorganizer.organizer

import com.arena.aifileorganizer.data.GeminiClient
import com.arena.aifileorganizer.model.AiFileDecision
import com.arena.aifileorganizer.model.CategoryPresets
import com.arena.aifileorganizer.model.FileContent
import com.arena.aifileorganizer.model.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Sends file batches (12 at a time) to Gemini and parses the JSON decisions.
 * Any parse/transport failure degrades to a deterministic extension-based
 * fallback so a scan NEVER crashes or loses files from the plan.
 */
class AiCategorizer(private val gemini: GeminiClient) {

    companion object {
        private const val BATCH_SIZE = 12
        private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|]")
    }

    suspend fun categorizeBatch(
        files: List<Pair<ScannedFile, FileContent>>
    ): Map<String, AiFileDecision> = withContext(Dispatchers.Default) {
        val result = mutableMapOf<String, AiFileDecision>()
        files.chunked(BATCH_SIZE).forEach { chunk ->
            ensureActive() // cooperative cancellation between batches
            result.putAll(categorizeChunk(chunk))
        }
        result
    }

    private suspend fun categorizeChunk(
        chunk: List<Pair<ScannedFile, FileContent>>
    ): Map<String, AiFileDecision> {
        val prompt = buildPrompt(chunk)
        val resp = gemini.generate(prompt)
        if (resp !is GeminiClient.Result.Ok) {
            val reason = if (resp is GeminiClient.Result.Err) "AI offline: ${resp.message}" else "fallback"
            return chunk.associate { it.first.uri.toString() to fallback(it.first, reason) }
        }

        // Strip markdown fences / prose — keep only the outermost JSON array.
        val text = resp.text
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) {
            return chunk.associate { it.first.uri.toString() to fallback(it.first, "respons AI tidak valid") }
        }

        return try {
            val arr = JSONArray(text.substring(start, end + 1))
            val map = mutableMapOf<String, AiFileDecision>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "$i").toIntOrNull() ?: i
                val file = chunk.getOrNull(id)?.first ?: continue
                map[file.uri.toString()] = toDecision(o, file)
            }
            // Gemini sometimes skips files — patch holes with the fallback.
            chunk.forEach { (f, _) -> map.getOrPut(f.uri.toString()) { fallback(f) } }
            map
        } catch (e: Exception) {
            chunk.associate { it.first.uri.toString() to fallback(it.first, "respons AI tidak valid") }
        }
    }

    private fun buildPrompt(chunk: List<Pair<ScannedFile, FileContent>>): String = buildString {
        appendLine("Kamu AI file organizer Indonesia. Klasifikasikan file.")
        appendLine("KATEGORI WAJIB: ${CategoryPresets.allowed.joinToString(", ")}")
        appendLine("Sub kategori: ${CategoryPresets.subFolders}")
        appendLine("Buat nama file RAPI: YYYY-MM-DD_JudulSingkat.Ekstensi")
        appendLine("JANGAN karakter ilegal: \\ / : * ? \" < > |")
        appendLine("Balas HANYA JSON array murni TANPA markdown/kode fence. Format:")
        appendLine("""[{"id":"0","category":"Dokumen_Kerja","sub":"PDF","newName":"2024-06-12_Laporan.pdf","confidence":0.92,"reason":"..."}]""")
        appendLine()
        chunk.forEachIndexed { idx, (file, content) ->
            appendLine("--- FILE id=$idx ---")
            appendLine("nama: ${file.displayName}")
            appendLine("mime: ${file.mimeType}")
            appendLine("size: ${file.size}")
            appendLine("modified: ${file.lastModified}")
            val preview = (content.ocrText ?: "") + "\n" + content.textPreview
            appendLine("isi_preview: ${preview.take(600)}")
            appendLine()
        }
    }

    private fun toDecision(o: org.json.JSONObject, file: ScannedFile): AiFileDecision {
        val catRaw = o.optString("category", "Lainnya")
        val category = CategoryPresets.allowed.find { it.equals(catRaw, ignoreCase = true) } ?: "Lainnya"
        val sub = o.optString("sub")
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        var newName = o.optString("newName", file.displayName)
            .replace(ILLEGAL_CHARS, "_")
            .trim()
            .ifBlank { file.displayName }
        if (!newName.contains('.')) {
            val ext = file.displayName.substringAfterLast('.', "")
            if (ext.isNotEmpty()) newName += ".$ext"
        }
        return AiFileDecision(
            category = category,
            subCategory = sub,
            newFileName = newName.take(120),
            confidence = o.optDouble("confidence", 0.7).toFloat(),
            reason = o.optString("reason", "-")
        )
    }

    private fun fallback(file: ScannedFile, reason: String = "fallback (aturan ekstensi)"): AiFileDecision {
        val ext = file.displayName.substringAfterLast('.', "").lowercase()
        val cat = when (ext) {
            "jpg", "jpeg", "png", "webp", "heic", "gif" -> "Foto_Pribadi"
            "mp4", "mkv", "mov", "3gp", "webm" -> "Video"
            "mp3", "m4a", "wav", "flac", "ogg", "aac" -> "Audio_Musik"
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "rtf" -> "Dokumen_Kerja"
            "epub", "mobi" -> "Ebook"
            "apk", "aab", "xapk" -> "APK_Installer"
            "zip", "rar", "7z", "tar", "gz" -> "Arsip_Project"
            else -> "Download_Random"
        }
        return AiFileDecision(cat, null, file.displayName, 0.4f, reason)
    }
}
