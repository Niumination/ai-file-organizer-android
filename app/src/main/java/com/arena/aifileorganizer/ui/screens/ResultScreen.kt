package com.arena.aifileorganizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arena.aifileorganizer.OrganizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(vm: OrganizerViewModel, onBackHome: () -> Unit) {
    val plan by vm.plan.collectAsState()
    val log by vm.executeLog.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Preview Plan - ${plan.size} file") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (log.isNotBlank()) { Text(log, style = MaterialTheme.typography.bodySmall) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.executePlan(dryRun = true) }, modifier = Modifier.weight(1f)) { Text("Dry-Run") }
                        Button(onClick = { vm.executePlan(dryRun = false) }, modifier = Modifier.weight(1f)) { Text("Eksekusi") }
                    }
                    OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) { Text("Selesai / Kembali") }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plan) { item ->
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.file.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("→ ${item.targetFolder}/${item.targetName}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        Text("${item.decision.category} • ${(item.decision.confidence*100).toInt()}% • ${item.decision.reason}", style = MaterialTheme.typography.bodySmall)
                        if (!item.content?.ocrText.isNullOrBlank()) {
                            Text("OCR: ${(item.content?.ocrText ?: "").take(120)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
