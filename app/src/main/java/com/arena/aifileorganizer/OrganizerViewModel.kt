package com.arena.aifileorganizer

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arena.aifileorganizer.data.ApiKeyStore
import com.arena.aifileorganizer.data.GeminiClient
import com.arena.aifileorganizer.model.OrganizePlanItem
import com.arena.aifileorganizer.organizer.AiCategorizer
import com.arena.aifileorganizer.organizer.ContentExtractor
import com.arena.aifileorganizer.organizer.FileMover
import com.arena.aifileorganizer.organizer.FileScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrganizerViewModel(app: Application, private val keyStore: ApiKeyStore) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val scanner = FileScanner(context)
    private val extractor = ContentExtractor(context)
    private val mover = FileMover(context)
    val treeUri = mutableStateOf<Uri?>(null)
    fun setTreeUri(uri: Uri) { treeUri.value = uri }
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState = _scanState.asStateFlow()
    private val _plan = MutableStateFlow<List<OrganizePlanItem>>(emptyList())
    val plan = _plan.asStateFlow()
    private val _executeLog = MutableStateFlow("")
    val executeLog = _executeLog.asStateFlow()
    fun startScan(maxFiles: Int = 150) {
        val uri = treeUri.value ?: return
        val apiKey = keyStore.getApiKey() ?: return
        val gemini = GeminiClient(apiKey)
        val categorizer = AiCategorizer(gemini)
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning("Memindai file...")
            val files = scanner.scanTree(uri, maxFiles = maxFiles)
            if (files.isEmpty()) { _scanState.value = ScanState.Error("Tidak ada file ditemukan"); return@launch }
            _scanState.value = ScanState.Scanning("Ekstrak konten ${files.size} file...")
            val withContent = files.mapIndexed { idx, f ->
                _scanState.value = ScanState.Scanning("Ekstrak ${idx+1}/${files.size}: ${f.displayName}")
                f to extractor.extract(f.uri, f.mimeType, f.displayName)
            }
            _scanState.value = ScanState.Scanning("Kirim ke Gemini AI...")
            val decisions = categorizer.categorizeBatch(withContent)
            val planList = withContent.map { (file, content) ->
                val decision = decisions[file.uri.toString()]!!
                val targetFolder = if (decision.subCategory != null) "${decision.category}/${decision.subCategory}" else decision.category
                OrganizePlanItem(file, content, decision, targetFolder, decision.newFileName)
            }
            _plan.value = planList
            _scanState.value = ScanState.Done
        }
    }
    fun executePlan(dryRun: Boolean = true) {
        val uri = treeUri.value ?: return
        val currentPlan = _plan.value
        viewModelScope.launch {
            _executeLog.value = "Mulai ${if (dryRun) "DRY-RUN" else "EKSEKUSI"}..."
            val res = mover.execute(currentPlan, uri, dryRun)
            _executeLog.value = (if (dryRun) "DRY-RUN " else "") + "Selesai. Sukses: ${res.success}, Gagal: ${res.failed}\n" + res.errors.joinToString("\n")
        }
    }
    sealed class ScanState {
        data object Idle: ScanState()
        data class Scanning(val msg: String): ScanState()
        data class Error(val msg: String): ScanState()
        data object Done: ScanState()
    }
}
