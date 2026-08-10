package com.arena.aifileorganizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.OrganizerViewModel
import com.arena.aifileorganizer.ui.rememberVisionHaptics
import com.arena.aifileorganizer.ui.theme.*
import kotlinx.coroutines.flow.first

/**
 * ScanScreen – visionOS spatial scanner.
 *
 * Anti stale-navigation: each scan gets a sequence id from the ViewModel and
 * we only navigate to the result when THE scan started by this composition
 * instance reports Done(seq).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreenVision(
    vm: OrganizerViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by vm.scanState.collectAsState()
    val haptics = rememberVisionHaptics()

    // Sequence id of the scan owned by this composition. startScan sets the
    // VM state to Running synchronously, so this can never piggyback on an
    // old Done state.
    var seq by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) { seq = vm.startScan(OrganizerViewModel.DEFAULT_MAX_FILES) }

    LaunchedEffect(seq) {
        if (seq <= 0) return@LaunchedEffect
        vm.scanState.first { (it as? OrganizerViewModel.ScanState.Done)?.seq == seq }
        onDone()
    }

    val bob = rememberVisionBob()
    val breathe = rememberVisionBreathe()
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "rot"
    )

    Scaffold(
        containerColor = VisionColors.paper,
        topBar = {
            TopAppBar(
                title = { Text("Memindai Berkas…", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = {
                        vm.cancelScan()
                        onBack()
                    }) { Text("← Kembali", color = VisionColors.muted) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Memindai\nberkas…",
                style = MaterialTheme.typography.headlineLarge,
                color = VisionColors.ink
            )

            // --- spatial scanner ---
            Box(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(corner = 28.dp, strong = true)
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            withTransform({ rotate(degrees = spin, pivot = center) }) {
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        0f to Color(0xFFB99CFF).copy(alpha = 0.0f),
                                        0.18f to Color(0xFFB99CFF).copy(alpha = 0.42f),
                                        0.5f to Color(0xFF7FD9FF).copy(alpha = 0.33f),
                                        0.82f to Color(0xFFB99CFF).copy(alpha = 0.0f),
                                        1f to Color(0xFFB99CFF).copy(alpha = 0.0f)
                                    ),
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 2.2f)
                                )
                            }
                        }
                        Box(
                            Modifier
                                .size(84.dp)
                                .graphicsLayer {
                                    translationY = bob * 0.65f
                                    scaleX = breathe
                                    scaleY = breathe
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.98f),
                                            Color(0xFFF5F0FF).copy(alpha = 0.72f),
                                            Color(0xFFDCEAFF).copy(alpha = 0.46f)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (state) {
                                    is OrganizerViewModel.ScanState.Running -> "AI"
                                    is OrganizerViewModel.ScanState.Done -> "✓"
                                    is OrganizerViewModel.ScanState.Error -> "!"
                                    else -> "…"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3A3452)
                            )
                        }
                        GlassMini("📄", Alignment.TopStart, Offset(-4f, 34f))
                        GlassMini("🖼️", Alignment.TopEnd, Offset(4f, 24f))
                        GlassMini("🧾", Alignment.BottomStart, Offset(28f, -4f))
                    }

                    Spacer(Modifier.height(18.dp))

                    when (val s = state) {
                        is OrganizerViewModel.ScanState.Running -> {
                            Text(s.phase, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            if (s.detail.isNotBlank()) {
                                Text(
                                    s.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VisionColors.muted,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LiquidProgressBar(
                                fraction = if (s.total > 0) {
                                    (s.current.toFloat() / s.total.toFloat())
                                } else {
                                    0.15f
                                }.coerceIn(0.02f, 0.98f)
                            )
                            if (s.total > 0) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${s.current} / ${s.total}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VisionColors.muted
                                )
                            }
                        }

                        is OrganizerViewModel.ScanState.Error -> {
                            Text(
                                "Gagal memindai",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = VisionColors.muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { haptics.light(); seq = vm.startScan() },
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF26232E),
                                    contentColor = Color(0xFFF5F2FF)
                                )
                            ) { Text("↻ Coba Lagi", fontWeight = FontWeight.SemiBold) }
                        }

                        else -> {
                            Text("Menyiapkan…", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            LiquidProgressBar(fraction = 0.12f)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ekstrak: PDF + OCR gambar + Gemini AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = VisionColors.muted
                    )
                }
            }

            // KPI – 2x2 glass (live from the plan as it builds)
            val planItems by vm.plan.collectAsState()
            val planSize = planItems.size
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricGlass("Berkas", if (planSize > 0) "$planSize" else "—", Modifier.weight(1f))
                MetricGlass(
                    "OCR",
                    if (planSize > 0) "${planItems.count { !it.content?.ocrText.isNullOrBlank() }}" else "—",
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val aiCount = planItems.count { it.decision.confidence > 0.5f }
                MetricGlass("AI", if (planSize > 0) "$aiCount" else "—", Modifier.weight(1f))
                MetricGlass(
                    "Kategori",
                    if (planSize > 0) "${planItems.map { it.decision.category }.distinct().size}" else "—",
                    Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = {
                    haptics.warning()
                    vm.cancelScan()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.38f),
                    contentColor = VisionColors.ink
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.82f), Color.White.copy(alpha = 0.44f))
                    )
                )
            ) { Text("Batalkan") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiquidProgressBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(999.dp))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB99CFF),
                            Color(0xFF7FD9FF),
                            Color(0xFFB6F07A)
                        )
                    )
                )
        )
    }
}

@Composable
private fun BoxScope.GlassMini(emoji: String, align: Alignment, offset: Offset) {
    Box(
        Modifier
            .align(align)
            .offset(x = offset.x.dp, y = offset.y.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .border(1.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 17.sp) }
}

@Composable
private fun MetricGlass(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .liquidGlass(corner = 16.dp)
            .padding(13.dp)
    ) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = VisionColors.ink)
        Text(label, fontSize = 11.sp, color = VisionColors.muted)
    }
}
