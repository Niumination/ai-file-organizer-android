package com.arena.aifileorganizer.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.arena.aifileorganizer.ui.rememberVisionHaptics
import com.arena.aifileorganizer.ui.theme.*
import kotlinx.coroutines.launch

/**
 * HomeScreen – visionOS Liquid Glass
 * Refs:
 * - SplineScene – serafim – https://21st.dev/r/serafimcloud/splite
 * - Apple Tahoe Liquid Glass Button – easemize – https://21st.dev/r/easemize/apple-tahoe-liquid-glass-button
 * - Liquid Glass Card – designali-in – https://21st.dev/r/designali-in/liquid-glass-card
 * - CardStack – ruixen.ui – https://21st.dev/r/ruixen.ui/card-stack
 * - Interactive 3D Character – dhiluxui – https://21st.dev/r/dhileepkumargm/interactive-3d-character
 * - Animated AI Chat – jatin-yadav05 – https://21st.dev/r/jatin-yadav05/animated-ai-chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenVision(
    apiKey: String?,
    treeUri: Uri?,
    onSaveApiKey: (String) -> Unit,
    onPickFolder: () -> Unit,
    onStartScan: () -> Unit,
    canStart: Boolean
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    val scroll = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberVisionHaptics()

    Scaffold(
        containerColor = VisionColors.paper,
        topBar = {
            // visionOS top ornament – liquid glass pill
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 18.dp, end = 18.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .liquidGlass(corner = 28.dp, strong = true)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // orb logo – Animated AI Chat style
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFB9A7FF),
                                            Color(0xFF8ADFFF),
                                            Color(0xFFFFD49A),
                                            Color(0xFFFFB7D6),
                                            Color(0xFFB9A7FF)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                        )
                        Column {
                            Text("AI File Organizer", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = VisionColors.ink)
                            Text("visionOS • Liquid Glass", fontSize = 10.5.sp, color = VisionColors.muted)
                        }
                    }
                    AssistChip(
                        onClick = { },
                        label = { Text("v1.1", fontSize = 10.5.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.52f),
                            labelColor = VisionColors.muted
                        ),
                        border = AssistChipDefaults.assistChipBorder(true,
                            borderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { Spacer(Modifier.height(4.dp)) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Spatial Hero — SplineScene style ---
            SpatialHero()

            Text(
                "Rapikan file",
                style = MaterialTheme.typography.displayLarge,
                color = VisionColors.ink
            )
            Text(
                "otomatis pakai AI",
                style = MaterialTheme.typography.displayLarge,
                color = VisionColors.accent
            )
            Text(
                "Gemini • PDF extract • ML Kit OCR • 100% SAF",
                style = MaterialTheme.typography.bodyMedium,
                color = VisionColors.muted
            )

            // KPI
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VisionKpi("1,284", "file di-scan", Modifier.weight(1f))
                VisionKpi("13", "kategori", Modifier.weight(1f))
                VisionKpi("98%", "akurat", Modifier.weight(1f))
            }

            // --- API Key — Liquid Glass Card — designali-in ---
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(corner = 20.dp, strong = false)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🔑  Gemini API Key", fontWeight = FontWeight.SemiBold, fontSize = 13.8.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.55f)
                    ) {
                        Text(" encrypted ", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.58f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.46f),
                        focusedBorderColor = Color(0xFFA98BFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.78f)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassButton(
                        text = "Simpan",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptics.tick()
                            onSaveApiKey(keyInput)
                            scope.launch { snackbar.showSnackbar("API key disimpan encrypted ✓") }
                        },
                        enabled = keyInput.isNotBlank()
                    )
                    OutlinedButton(
                        onClick = { /* open browser elsewhere */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.38f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.4f))
                            )
                        )
                    ) { Text("Gratis →", fontSize = 12.8.sp, color = VisionColors.ink) }
                }
                Text(
                    "Disimpan EncryptedSharedPreferences • open source",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // --- Folder picker – CardStack — ruixen.ui ---
            Text(
                "Folder sumber",
                style = MaterialTheme.typography.labelSmall,
                color = VisionColors.muted,
                letterSpacing = 0.8.sp
            )
            VisionCardStack(
                pickedUri = treeUri?.toString(),
                onPick = onPickFolder
            )

            Spacer(Modifier.height(4.dp))

            // --- CTA — Apple Tahoe Liquid Glass Button — easemize ---
            LiquidGlassButton(
                text = "Mulai Scan & Analisis AI  →",
                dark = true,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    haptics.light()
                    onStartScan()
                }
            )
            if (!canStart) {
                Text(
                    "Isi API Key dan pilih folder dulu.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Text(
                "Output → AI_Organized/  •  Preview  •  Dry-run",
                style = MaterialTheme.typography.bodySmall,
                color = VisionColors.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SpatialHero() {
    // SplineScene style + Interactive 3D Character
    var rx by remember { mutableStateOf(0f) }
    var ry by remember { mutableStateOf(0f) }
    val bob = rememberVisionBob()
    val breathe = rememberVisionBreathe()

    Box(
        Modifier
            .fillMaxWidth()
            .height(222.dp)
            .liquidGlass(corner = 28.dp, strong = true)
            .visionAurora()
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    ry = (ry + drag.x * 0.045f).coerceIn(-14f, 14f)
                    rx = (rx - drag.y * 0.045f).coerceIn(-10f, 10f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // floating glass chips – Liquid Glass Button style
        GlassChip("PDF", Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = 22.dp))
        GlassChip("OCR", Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 30.dp))
        GlassChip("Gemini", Modifier.align(Alignment.BottomStart).offset(x = 36.dp, y = (-22).dp))

        // liquid orb – Animated AI Chat glass-morphism
        Box(
            Modifier
                .size(112.dp)
                .visionTilt(rx, ry)
                .graphicsLayer {
                    translationY = bob
                    scaleX = breathe
                    scaleY = breathe
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.97f),
                            Color.White.copy(alpha = 0.52f),
                            Color(0xFFD2C8FF).copy(alpha = 0.34f),
                            Color(0xFFAAD8FF).copy(alpha = 0.26f)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.88f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // specular
            Box(
                Modifier
                    .offset(y = (-18).dp)
                    .size(width = 56.dp, height = 30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.92f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Text(
                "AI",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Color(0xFF3B3352)
            )
        }

        // bottom meta
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SAF • Scoped Storage", style = MaterialTheme.typography.labelSmall, color = VisionColors.muted)
            Text("Gemini 1.5 Flash", style = MaterialTheme.typography.labelSmall, color = VisionColors.muted)
        }
    }
}

@Composable
private fun GlassChip(text: String, modifier: Modifier = Modifier) {
    val bob = rememberVisionBob()
    val off = when (text) {
        "PDF" -> 0f
        "OCR" -> 2.3f
        else -> 1.1f
    }
    Surface(
        modifier = modifier.graphicsLayer { translationY = bob * 0.45f + off },
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.52f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.74f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF45444A)
        )
    }
}

@Composable
private fun VisionKpi(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .liquidGlass(corner = 16.dp)
            .padding(vertical = 13.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = VisionColors.ink)
        Text(label, fontSize = 11.sp, color = VisionColors.muted)
    }
}

@Composable
private fun VisionCardStack(pickedUri: String?, onPick: () -> Unit) {
    val haptics = rememberVisionHaptics()
    // CardStack – ruixen.ui
    Box(
        Modifier
            .fillMaxWidth()
            .height(134.dp)
    ) {
        val items = listOf(
            Triple("📁 /Android", "— blocked", 2),
            Triple("📁 /DCIM", "— 412 foto", 1),
            Triple(
                pickedUri?.let { "📂 ${it.takeLast(32)}" } ?: "📂 Pilih folder…",
                "SAF • user-granted only",
                0
            )
        )
        items.forEach { (title, sub, idx) ->
            val s = visionStackAt(idx)
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = s.scale
                        scaleY = s.scale
                        translationY = s.y
                        rotationZ = s.rot
                        alpha = s.alpha
                    }
                    .liquidGlass(corner = 18.dp, strong = idx == 0)
                    .padding(14.dp)
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.3.sp,
                        color = if (idx==0) VisionColors.ink else VisionColors.muted)
                    Text(sub, style = MaterialTheme.typography.bodySmall,
                        color = if (idx==0) VisionColors.muted else VisionColors.muted.copy(alpha = 0.8f))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { haptics.tick(); onPick() },
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
    ) {
        Text("Pilih Folder di Storage / SD Card", fontWeight = FontWeight.Medium)
    }
    Text(
        "App HANYA akses folder yang kamu pilih. /Android otomatis di-skip.",
        style = MaterialTheme.typography.bodySmall,
        color = VisionColors.muted,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * Liquid Glass Button – Apple Tahoe
 * Port of: https://21st.dev/r/easemize/apple-tahoe-liquid-glass-button
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dark: Boolean = false
) {
    var hovered by remember { mutableStateOf(false) }
    val sheen = rememberLiquidSheen(hovered)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .liquidGlassButton(dark = dark),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (dark) Color(0xFFF5F3FF) else VisionColors.ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = VisionColors.muted
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
