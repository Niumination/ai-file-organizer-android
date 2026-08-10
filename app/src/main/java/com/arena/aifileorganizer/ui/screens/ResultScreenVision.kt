package com.arena.aifileorganizer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.OrganizerViewModel
import com.arena.aifileorganizer.ui.VisionConfetti
import com.arena.aifileorganizer.ui.rememberVisionHaptics
import com.arena.aifileorganizer.ui.theme.*
import java.util.Locale

/**
 * ResultScreen – visionOS Liquid Glass.
 * Preview plan + dry-run + eksekusi dengan progress nyata dan guard anti
 * double-execution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenVision(
    vm: OrganizerViewModel,
    onBackHome: () -> Unit
) {
    val plan by vm.plan.collectAsState()
    val execState by vm.executeState.collectAsState()
    var filter by remember { mutableStateOf("Semua") }
    val cats = remember(plan) { listOf("Semua") + plan.map { it.decision.category }.distinct() }
    val snackbar = remember { SnackbarHostState() }
    val haptics = rememberVisionHaptics()
    var showConfetti by remember { mutableStateOf(false) }

    val executing = execState is OrganizerViewModel.ExecuteState.Running

    // Confetti on a successful real run.
    LaunchedEffect(execState) {
        val f = execState as? OrganizerViewModel.ExecuteState.Finished ?: return@LaunchedEffect
        if (!f.dryRun && f.success > 0) {
            haptics.success()
            showConfetti = true
        }
    }

    Scaffold(
        containerColor = VisionColors.paper,
        topBar = {
            TopAppBar(
                title = { Text("Pratinjau • ${plan.size} berkas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = {
                        haptics.light()
                        onBackHome()
                    }) { Text("← Home", color = VisionColors.muted) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(corner = 0.dp, strong = true)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (val s = execState) {
                    is OrganizerViewModel.ExecuteState.Running -> {
                        val pct = if (s.total > 0) s.current.toFloat() / s.total.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { pct.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF8A7BD8),
                            trackColor = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            (if (s.dryRun) "Uji coba" else "Memindahkan") +
                                " ${s.current}/${s.total}…",
                            style = MaterialTheme.typography.bodySmall,
                            color = VisionColors.muted
                        )
                    }

                    is OrganizerViewModel.ExecuteState.Finished -> {
                        Text(
                            if (s.dryRun) {
                                "✓ Uji coba selesai: ${s.success} siap dipindah" +
                                    (if (s.failed > 0) ", ${s.failed} gagal" else "")
                            } else {
                                "⚡ Eksekusi selesai: ${s.success} berhasil" +
                                    (if (s.failed > 0) ", ${s.failed} gagal" else "")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (s.failed > 0) VisionColors.ink else VisionColors.success
                        )
                        if (s.errors.isNotEmpty()) {
                            Text(
                                s.errors.take(3).joinToString("\n") +
                                    if (s.errors.size > 3) "\n…dan ${s.errors.size - 3} lainnya" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    else -> Unit
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { haptics.tick(); vm.executePlan(dryRun = true) },
                        modifier = Modifier.weight(1f),
                        enabled = !executing && plan.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.42f),
                            contentColor = VisionColors.ink
                        )
                    ) { Text("🧪 Uji Coba", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

                    Button(
                        onClick = { haptics.light(); vm.executePlan(dryRun = false) },
                        modifier = Modifier.weight(1f),
                        enabled = !executing && plan.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF26232E),
                            contentColor = Color(0xFFF5F2FF)
                        )
                    ) { Text("⚡ Eksekusi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // Success summary after a real run consumed the plan.
            val finished = execState as? OrganizerViewModel.ExecuteState.Finished
            if (plan.isEmpty()) {
                EmptyResultState(
                    finished = finished,
                    onBackHome = onBackHome
                )
            } else {
                // filter chips – Liquid Glass Button style
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cats.forEach { c ->
                        val on = filter == c
                        Surface(
                            onClick = { haptics.tick(); filter = c },
                            shape = RoundedCornerShape(999.dp),
                            color = if (on) Color(0xFF26232E) else Color.White.copy(alpha = 0.52f),
                            tonalElevation = 0.dp,
                            shadowElevation = if (on) 4.dp else 0.dp,
                            border = if (!on) androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = 0.74f)
                            ) else null
                        ) {
                            Text(
                                c,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (on) Color(0xFFF5F2FF) else Color(0xFF55535A)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))

                val filtered = if (filter == "Semua") plan else plan.filter { it.decision.category == filter }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filtered, key = { _, it -> it.file.uri.toString() }) { _, item ->
                        PlanCard(item)
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showConfetti) {
        VisionConfetti(
            visible = true,
            modifier = Modifier.fillMaxSize(),
            onFinish = { showConfetti = false }
        )
    }
}

@Composable
private fun PlanCard(item: com.arena.aifileorganizer.model.OrganizePlanItem) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(corner = 18.dp, strong = false)
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.60f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.88f))
            ) {
                Text(
                    "${item.decision.category} • ${(item.decision.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 10.7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A4558),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Text(
                humanSize(item.file.size),
                fontSize = 10.7.sp,
                color = VisionColors.muted,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.file.displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.6.sp,
            color = VisionColors.ink,
            maxLines = 2
        )
        Text(
            "↓ ${item.targetFolder}/${item.targetName}",
            color = VisionColors.success,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.8.sp,
            maxLines = 1
        )
        Text(
            item.decision.reason,
            style = MaterialTheme.typography.bodySmall,
            color = VisionColors.muted,
            maxLines = 2
        )
        if (!item.content?.ocrText.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "OCR: " + (item.content?.ocrText ?: "").take(120),
                fontSize = 11.3.sp,
                color = VisionColors.ocr,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun EmptyResultState(
    finished: OrganizerViewModel.ExecuteState.Finished?,
    onBackHome: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .liquidGlass(corner = 24.dp, strong = true)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (finished != null && !finished.dryRun && finished.success > 0) "✔\uFE0F"
                else "📂",
                fontSize = 44.sp
            )
            if (finished != null) {
                Text(
                    if (finished.dryRun) "Uji coba selesai" else "Semua berkas sudah dirapikan!",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = VisionColors.ink
                )
                Text(
                    buildString {
                        append("${finished.success} berhasil")
                        if (finished.failed > 0) append(" • ${finished.failed} gagal")
                        append("\nHasil ada di folder AI_Organized/")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VisionColors.muted
                )
            } else {
                Text(
                    "Belum ada rencana",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = VisionColors.ink
                )
                Text(
                    "Jalankan scan dulu dari halaman utama.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VisionColors.muted
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onBackHome,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF26232E),
                    contentColor = Color(0xFFF5F2FF)
                )
            ) { Text("← Kembali ke Home", fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}
