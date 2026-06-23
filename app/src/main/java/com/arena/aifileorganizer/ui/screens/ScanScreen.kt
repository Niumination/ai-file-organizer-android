package com.arena.aifileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.aifileorganizer.OrganizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: OrganizerViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val state by vm.scanState.collectAsState()
    LaunchedEffect(Unit) { vm.startScan(maxFiles = 150) }
    LaunchedEffect(state) { if (state is OrganizerViewModel.ScanState.Done) onDone() }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Scanning...") }) }) {pad ->
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
                when (val s = state) {
                    is OrganizerViewModel.ScanState.Scanning -> {
                        CircularProgressIndicator(); Text(s.msg)
                        Text("Full Content: PDF extract + OCR gambar + Gemini", style = MaterialTheme.typography.bodySmall)
                    }
                    is OrganizerViewModel.ScanState.Error -> {
                        Text("Error: ${s.msg}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = onBack) { Text("Kembali") }
                    }
                    is OrganizerViewModel.ScanState.Done -> { Text("Selesai! Menampilkan hasil...") }
                    else -> { CircularProgressIndicator() }
                }
            }
        }
    }
}
