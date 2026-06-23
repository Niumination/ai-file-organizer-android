package com.arena.aifileorganizer.model

import android.net.Uri
import kotlinx.serialization.Serializable

data class ScannedFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val relativePath: String
)

data class FileContent(
    val textPreview: String,
    val ocrText: String? = null,
    val imageLabels: List<String> = emptyList()
)

data class AiFileDecision(
    val category: String,
    val subCategory: String? = null,
    val newFileName: String,
    val confidence: Float,
    val reason: String
)

data class OrganizePlanItem(
    val file: ScannedFile,
    val content: FileContent?,
    val decision: AiFileDecision,
    val targetFolder: String,
    val targetName: String
)

@Serializable data class GeminiPart(val text: String)
@Serializable data class GeminiContent(val parts: List<GeminiPart>)
@Serializable data class GeminiRequest(val contents: List<GeminiContent>)
@Serializable data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)
@Serializable data class GeminiCandidate(val content: GeminiContent? = null)

object CategoryPresets {
    val allowed = listOf(
        "Dokumen_Kerja","Dokumen_Pribadi","Struk_Invoice","Foto_Pribadi",
        "Foto_Keluarga","Screenshot","Video","Audio_Musik","Ebook",
        "Arsip_Project","APK_Installer","Download_Random","Lainnya"
    )
    val subFolders = mapOf(
        "Dokumen_Kerja" to listOf("PDF","Word","Excel","Presentasi"),
        "Dokumen_Pribadi" to listOf("KTP_SIM","Sertifikat","Lainnya"),
        "Struk_Invoice" to listOf("Tokopedia","Shopee","Gojek","Lainnya"),
        "Ebook" to listOf("PDF","EPUB"),
        "Foto_Pribadi" to listOf("Kamera","WhatsApp","Edit"),
        "Video" to listOf("Kamera","Download","Lainnya")
    )
}
