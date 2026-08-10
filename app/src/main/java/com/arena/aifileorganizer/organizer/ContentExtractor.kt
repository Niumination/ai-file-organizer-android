package com.arena.aifileorganizer.organizer

import android.content.Context
import android.net.Uri
import com.arena.aifileorganizer.model.FileContent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Extracts a short text preview for the AI prompt:
 * - TXT/MD/CSV/JSON → read directly
 * - PDF → first 2 pages via PDFBox
 * - Images → on-device OCR via ML Kit (works offline after first model download)
 * - Everything else → name + mime only
 */
class ContentExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @Volatile
    private var pdfBoxInit = false

    suspend fun extract(uri: Uri, mime: String?, displayName: String): FileContent =
        withContext(Dispatchers.IO) {
            val lower = displayName.lowercase()
            try {
                when {
                    mime?.startsWith("text/") == true ||
                        lower.endsWith(".txt") || lower.endsWith(".md") ||
                        lower.endsWith(".json") || lower.endsWith(".csv") ||
                        lower.endsWith(".log") ->
                        FileContent(textPreview = readText(uri, 1800))

                    mime == "application/pdf" || lower.endsWith(".pdf") ->
                        FileContent(textPreview = extractPdf(uri))

                    mime?.startsWith("image/") == true ||
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") || lower.endsWith(".webp") -> {
                        val ocr = ocrImage(uri)
                        FileContent(
                            textPreview = "GAMBAR: $displayName",
                            ocrText = ocr.takeIf { it.isNotBlank() }
                        )
                    }

                    else -> FileContent(textPreview = "Nama: $displayName | MIME: $mime")
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                FileContent(textPreview = "Nama: $displayName | MIME: $mime | err")
            }
        }

    private fun readText(uri: Uri, maxChars: Int): String {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            BufferedReader(InputStreamReader(ins)).use { br ->
                val buf = CharArray(minOf(2048, maxChars))
                val sb = StringBuilder()
                while (sb.length < maxChars) {
                    val read = br.read(buf) ?: break
                    if (read <= 0) break
                    sb.append(buf, 0, minOf(read, maxChars - sb.length))
                }
                return sb.toString()
            }
        }
        return ""
    }

    private fun extractPdf(uri: Uri): String {
        synchronized(this) {
            if (!pdfBoxInit) {
                PDFBoxResourceLoader.init(context)
                pdfBoxInit = true
            }
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                PDDocument.load(ins).use { doc ->
                    val stripper = PDFTextStripper()
                    stripper.startPage = 1
                    stripper.endPage = kotlin.math.min(2, doc.numberOfPages)
                    stripper.getText(doc).take(1800)
                }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun ocrImage(uri: Uri): String = withContext(Dispatchers.Default) {
        try {
            val input = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(input).await()
            result.text.take(1200)
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (_: Exception) {
            ""
        }
    }

    /** Releases the ML Kit recognizer. Call when the owner is cleared. */
    fun close() {
        recognizer.close()
    }
}
