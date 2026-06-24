package com.arena.aifileorganizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.OrganizerViewModel
import com.arena.aifileorganizer.ui.VisionConfetti
import com.arena.aifileorganizer.ui.VisionHaptics
import com.arena.aifileorganizer.ui.rememberVisionHaptics
import com.arena.aifileorganizer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ResultScreen – visionOS Liquid Glass
 * - Filter chips: Liquid Glass Button style
 * - Plan cards: Liquid Glass Card – designali-in
 * - Confetti: Liquid Effect Animation – thanh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenVision(
    vm: OrganizerViewModel,
    onBackHome: () -> Unit
) {
    val plan by vm.plan.collectAsState()
    val log by vm.executeLog.collectAsState()
    var filter by remember { mutableStateOf("Semua") }
    val cats = remember(plan) { listOf("Semua") + plan.map { it.decision.category }.distinct() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val haptics = rememberVisionHaptics()
    var showConfetti by remember { mutableStateOf(false) }

    // auto confetti when execute finishes successfully
    val logText = log
    LaunchedEffect(logText) {
        if (logText.contains("Sukses:") && !logText.contains("DRY-RUN") && !logText.contains("0," , true).not()) {
            // naive: if success >0
            val successNum = Regex("""Sukses:\s*(\d+)""").find(logText)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            if (successNum > 0) {
                haptics.success()
                showConfetti = true
            }
        }
    }

    Box {
    Scaffold(
        containerColor = VisionColors.paper,
        topBar = {
            TopAppBar(
                title = { Text("Preview Plan • ${plan.size} file", fontWeight = FontWeight.SemiBold) },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (log.isNotBlank()) {
                    Text(log, style = MaterialTheme.typography.bodySmall, color = VisionColors.muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { haptics.tick(); vm.executePlan(dryRun = true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.42f),
                            contentColor = VisionColors.ink
                        )
                    ) { Text("🧪 Dry-Run", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

                    Button(
                        onClick = { haptics.light(); vm.executePlan(dryRun = false) },
                        modifier = Modifier.weight(1f),
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
            // filter chips – Liquid Glass Button style
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
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
            Spacer(Modifier.height(10.dp))

            val filtered = if (filter == "Semua") plan else plan.filter { it.decision.category == filter }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filtered, key = { _, it -> it.file.uri.toString() }) { idx, item ->
                    val rot = if (idx % 2 == 0) -0.18f else 0.18f
                    // Liquid Glass Card – designali-in
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer { rotationZ = rot }
                            .liquidGlass(corner = 18.dp, strong = false)
                            .padding(14.dp)
                    ) {
                        // tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.60f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = 0.88f)
                            )
                        ) {
                            Text(
                                "${item.decision.category} • ${(item.decision.confidence*100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                fontSize = 10.7.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4A4558),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.file.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.6.sp, color = VisionColors.ink)
                        Text(
                            "↓ ${item.targetFolder}/${item.targetName}",
                            color = VisionColors.success,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.8.sp
                        )
                        Text(item.decision.reason, style = MaterialTheme.typography.bodySmall, color = VisionColors.muted)
                        if (!item.content?.ocrText.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "OCR: " + (item.content?.ocrText ?: "").take(120),
                                fontSize = 11.3.sp,
                                color = VisionColors.ocr,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
            }
    }

        // Lottie confetti overlay – visionOS
        if (showConfetti) {
            VisionConfetti(
                visible = true,
                modifier = Modifier.fillMaxSize(),
                onFinish = { showConfetti = false }
            )
        }
}
