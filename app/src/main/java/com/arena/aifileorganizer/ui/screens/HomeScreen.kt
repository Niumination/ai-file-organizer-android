package com.arena.aifileorganizer.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    apiKey: String?,
    treeUri: Uri?,
    onSaveApiKey: (String) -> Unit,
    onPickFolder: () -> Unit,
    onStartScan: () -> Unit,
    canStart: Boolean
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    Scaffold(topBar = { TopAppBar(title = { Text("AI File Organizer", fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Rapikan file HP/SD Card pakai Gemini AI", style = MaterialTheme.typography.titleMedium)
            Text("✅ 100% SAF - tidak menyentuh file sistem\n✅ OCR struk/invoice otomatis\n✅ Preview sebelum eksekusi", style = MaterialTheme.typography.bodySmall)
            OutlinedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. API Key Gemini", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = keyInput, onValueChange = { keyInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("AIza...") }, singleLine = true)
                    Button(onClick = { onSaveApiKey(keyInput) }, enabled = keyInput.isNotBlank()) { Text("Simpan Key (encrypted)") }
                    Text("Gratis di aistudio.google.com/app/apikey", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Pilih Folder Sumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (treeUri != null) "Terpilih: $treeUri" else "Belum pilih folder", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onPickFolder) { Text("Pilih Folder di Storage / SD Card") }
                    Text("App HANYA bisa akses folder yang kamu pilih. Folder sistem /Android otomatis di-skip.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Button(onClick = onStartScan, enabled = canStart, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Mulai Scan & Analisis AI", style = MaterialTheme.typography.titleMedium) }
            if (!canStart) { Text("Isi API Key dan pilih folder dulu.", color = MaterialTheme.colorScheme.error) }
            Divider()
            Text("Output: [FolderPilihan]/AI_Organized/\nDokumen_Kerja/, Foto_Pribadi/, Struk_Invoice/, dll", style = MaterialTheme.typography.bodySmall)
        }
    }
}
