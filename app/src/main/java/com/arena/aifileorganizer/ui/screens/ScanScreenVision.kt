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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.OrganizerViewModel
import com.arena.aifileorganizer.ui.theme.*

/**
 * ScanScreen – visionOS spatial scanner
 * Refs:
 * - Container Scroll Animation – aceternity – https://21st.dev/r/aceternity/container-scroll-animation
 * - Animated AI Chat (glass-morphism) – jatin-yadav05 – https://21st.dev/r/jatin-yadav05/animated-ai-chat
 * - Liquid Effect Animation – thanh – 264★
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreenVision(
    vm: OrganizerViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by vm.scanState.collectAsState()
    LaunchedEffect(Unit) { vm.startScan(maxFiles = 150) }
    LaunchedEffect(state) { if (state is OrganizerViewModel.ScanState.Done) onDone() }

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
                title = { Text("AI Scanning…", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Kembali", color = VisionColors.muted) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Scanning\nfiles…",
                style = MaterialTheme.typography.headlineLarge,
                color = VisionColors.ink
            )

            // --- spatial scanner — Container Scroll Animation + Animated AI Chat ---
            Box(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(corner = 28.dp, strong = true)
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // orbit
                    Box(
                        Modifier.size(188.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // conic glow ring
                        Canvas(Modifier.fillMaxSize()) {
                            rotate(degrees = spin, pivot = center) {
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
                        // core – Liquid Effect Animation style
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
                                    is OrganizerViewModel.ScanState.Scanning -> "AI"
                                    is OrganizerViewModel.ScanState.Done -> "✓"
                                    else -> "…"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3A3452)
                            )
                        }
                        // floating glass chips
                        GlassMini("📄", Alignment.TopStart, Offset(-4f, 34f))
                        GlassMini("🖼️", Alignment.TopEnd, Offset(4f, 24f))
                        GlassMini("🧾", Alignment.BottomStart, Offset(28f, -4f))
                    }

                    Spacer(Modifier.height(18.dp))

                    val msg = when (val s = state) {
                        is OrganizerViewModel.ScanState.Scanning -> s.msg
                        is OrganizerViewModel.ScanState.Error -> "Error: ${s.msg}"
                        is OrganizerViewModel.ScanState.Done -> "Selesai!"
                        else -> "Menyiapkan…"
                    }
                    Text(msg, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))

                    // liquid progress
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.52f))
                            .border(1.dp, Color.White.copy(alpha = 0.84f), RoundedCornerShape(999.dp))
                    ) {
                        val pct = when (state) {
                            is OrganizerViewModel.ScanState.Scanning -> 0.45f
                            is OrganizerViewModel.ScanState.Done -> 1f
                            else -> 0.12f
                        }
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct)
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

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Full Content: PDF extract + OCR gambar + Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = VisionColors.muted
                    )
                }
            }

            // KPI – 2x2 glass
            val planSize = vm.plan.collectAsState().value.size
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricGlass("File", if (planSize>0) "$planSize" else "—", Modifier.weight(1f))
                MetricGlass("OCR", "—", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricGlass("AI", "—", Modifier.weight(1f))
                MetricGlass("Token", "—", Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = onBack,
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

            Spacer(Modifier.height(60.dp))
        }
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
