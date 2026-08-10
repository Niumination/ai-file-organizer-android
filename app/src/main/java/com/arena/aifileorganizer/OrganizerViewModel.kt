package com.arena.aifileorganizer

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.aifileorganizer.data.ApiKeyStore
import com.arena.aifileorganizer.data.GeminiClient
import com.arena.aifileorganizer.model.AiFileDecision
import com.arena.aifileorganizer.model.FileContent
import com.arena.aifileorganizer.model.OrganizePlanItem
import com.arena.aifileorganizer.model.ScannedFile
import com.arena.aifileorganizer.organizer.AiCategorizer
import com.arena.aifileorganizer.organizer.ContentExtractor
import com.arena.aifileorganizer.organizer.FileMover
import com.arena.aifileorganizer.organizer.FileScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

/**
 * Drives the whole pipeline: Scan → Extract → AI Categorize → Plan → Move.
 *
 * Production guarantees:
 * - Every failure lands in [ScanState.Error] / [ExecuteState.Finished] — never crashes.
 * - Each scan gets a monotonic sequence id so screens ignore stale results.
 * - Scans and executions are cancellable and cannot overlap.
 */
class OrganizerViewModel(app: Application, private val keyStore: ApiKeyStore) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "OrganizerVM"
        const val DEFAULT_MAX_FILES = 150
    }

    private val context = app.applicationContext
    private val scanner = FileScanner(context)
    private val extractor = ContentExtractor(context)
    private val mover = FileMover(context)

    val treeUri = mutableStateOf<Uri?>(null)
    fun setTreeUri(uri: Uri?) {
        treeUri.value = uri
    }

    // ------------------------------------------------------------ Scan state

    sealed interface ScanState {
        data object Idle : ScanState
        data class Running(
            val phase: String,
            val current: Int,
            val total: Int,
            val detail: String = ""
        ) : ScanState

        data class Error(val message: String) : ScanState

        /** [seq] identifies the scan run so UI can ignore stale completions. */
        data class Done(val seq: Int, val count: Int) : ScanState
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState = _scanState.asStateFlow()
    private val _scanSeq = MutableStateFlow(0)
    val scanSeq = _scanSeq.asStateFlow()
    private var scanJob: Job? = null

    private val _plan = MutableStateFlow<List<OrganizePlanItem>>(emptyList())
    val plan = _plan.asStateFlow()

    // --------------------------------------------------------- Execute state

    sealed interface ExecuteState {
        data object Idle : ExecuteState
        data class Running(val current: Int, val total: Int, val dryRun: Boolean) : ExecuteState
        data class Finished(
            val success: Int,
            val failed: Int,
            val dryRun: Boolean,
            val errors: List<String>
        ) : ExecuteState
    }

    private val _executeState = MutableStateFlow<ExecuteState>(ExecuteState.Idle)
    val executeState = _executeState.asStateFlow()
    private var executeJob: Job? = null

    val isExecuting: Boolean get() = _executeState.value is ExecuteState.Running

    // -------------------------------------------------------------- Scanning

    /**
     * Starts a scan. Returns the scan sequence id (>0) or 0 if preconditions fail.
     * Sets a terminal [ScanState] before launching so UI never acts on stale data.
     */
    @Synchronized
    fun startScan(maxFiles: Int = DEFAULT_MAX_FILES): Int {
        val uri = treeUri.value
        val apiKey = keyStore.getApiKey()?.takeIf { it.isNotBlank() }
        if (uri == null || apiKey == null) {
            _scanState.value = ScanState.Error("Pilih folder sumber dan isi API key terlebih dahulu.")
            return 0
        }

        val seq = _scanSeq.value + 1
        _scanSeq.value = seq
        _executeState.value = ExecuteState.Idle
        _plan.value = emptyList()
        _scanState.value = ScanState.Running("Memindai folder…", 0, 0)

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val files = scanner.scanTree(uri, maxFiles = maxFiles)
                if (files.isEmpty()) {
                    _scanState.value = ScanState.Error(
                        "Tidak ada file di folder ini (folder sistem & folder hasil AI_Organized otomatis dilewati)."
                    )
                    return@launch
                }

                _scanState.value = ScanState.Running("Ekstrak konten…", 0, files.size)
                val extracted = ArrayList<Pair<ScannedFile, FileContent>>(files.size)
                files.forEachIndexed { idx, f ->
                    _scanState.value =
                        ScanState.Running("Ekstrak konten…", idx + 1, files.size, f.displayName)
                    extracted.add(f to extractor.extract(f.uri, f.mimeType, f.displayName))
                }

                _scanState.value = ScanState.Running("Analisis Gemini AI…", files.size, files.size)
                val decisions = AiCategorizer(GeminiClient(apiKey)).categorizeBatch(extracted)

                val planList = extracted.map { (file, content) ->
                    val decision = decisions[file.uri.toString()] ?: fallbackDecision(file)
                    val targetFolder = if (decision.subCategory != null) {
                        "${decision.category}/${decision.subCategory}"
                    } else {
                        decision.category
                    }
                    OrganizePlanItem(file, content, decision, targetFolder, decision.newFileName)
                }
                _plan.value = planList
                _scanState.value = ScanState.Done(seq, planList.size)
            } catch (ce: CancellationException) {
                _scanState.value = ScanState.Idle
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "scan failed", e)
                _scanState.value = ScanState.Error(friendlyError(e))
            }
        }
        return seq
    }

    /** Cancels a running scan. Safe to call anytime. */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        if (_scanState.value is ScanState.Running) _scanState.value = ScanState.Idle
    }

    // ------------------------------------------------------------- Execution

    /** Guarded: a second tap while running is ignored — no double execution. */
    fun executePlan(dryRun: Boolean) {
        val uri = treeUri.value ?: return
        val current = _plan.value
        if (current.isEmpty()) return
        if (_executeState.value is ExecuteState.Running) return

        executeJob = viewModelScope.launch {
            try {
                _executeState.value = ExecuteState.Running(0, current.size, dryRun)
                val res = mover.execute(current, uri, dryRun) { done, total ->
                    _executeState.value = ExecuteState.Running(done, total, dryRun)
                }
                _executeState.value = ExecuteState.Finished(res.success, res.failed, dryRun, res.errors)
                // Confirmed moves are consumed so they can never run twice.
                if (!dryRun && res.success > 0) _plan.value = emptyList()
            } catch (ce: CancellationException) {
                _executeState.value = ExecuteState.Idle
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "execute failed", e)
                _executeState.value = ExecuteState.Finished(
                    success = 0,
                    failed = current.size,
                    dryRun = dryRun,
                    errors = listOf(friendlyError(e))
                )
            }
        }
    }

    fun resetExecuteState() {
        if (_executeState.value !is ExecuteState.Running) _executeState.value = ExecuteState.Idle
    }

    // ---------------------------------------------------------------- Helpers

    private fun fallbackDecision(file: ScannedFile): AiFileDecision {
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
        return AiFileDecision(cat, null, file.displayName, 0.4f, "fallback (aturan ekstensi)")
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is UnknownHostException -> "Tidak ada koneksi internet."
        is SecurityException -> "Izin akses folder dicabut — pilih ulang folder."
        is IOException -> "Gangguan baca/tulis storage: ${e.message ?: e.javaClass.simpleName}"
        else -> e.message?.take(140) ?: "Error tidak dikenal (${e.javaClass.simpleName})"
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        executeJob?.cancel()
        extractor.close()
    }
}
