package com.arena.aifileorganizer.ui.screens

import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.ui.rememberVisionHaptics
import com.arena.aifileorganizer.ui.theme.*
import kotlinx.coroutines.launch

private const val AI_STUDIO_URL = "https://aistudio.google.com/app/apikey"

/**
 * HomeScreen – visionOS Liquid Glass.
 * Setup API key + folder sumber + tombol mulai scan.
 */
@Composable
fun HomeScreenVision(
    apiKey: String?,
    treeUri: Uri?,
    folderLabel: String?,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onStartScan: () -> Unit,
    canStart: Boolean
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    var keyVisible by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberVisionHaptics()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = VisionColors.paper,
        topBar = {
            // visionOS top ornament – liquid glass pill
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                            Text(
                                "AI File Organizer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                color = VisionColors.ink
                            )
                            Text(
                                "visionOS • Liquid Glass",
                                fontSize = 10.5.sp,
                                color = VisionColors.muted
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.52f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            "v1.2",
                            fontSize = 10.5.sp,
                            color = VisionColors.muted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .imePadding()
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Spatial Hero — SplineScene style ---
            SpatialHero()

            Text(
                "Rapikan file",
                style = MaterialTheme.typography.displayLarge,
                color = VisionColors.ink,
                maxLines = 2
            )
            Text(
                "otomatis pakai AI",
                style = MaterialTheme.typography.displayLarge,
                color = VisionColors.accent,
                maxLines = 1
            )
            Text(
                "Gemini • PDF • OCR on-device • 100% aman dengan SAF",
                style = MaterialTheme.typography.bodyMedium,
                color = VisionColors.muted
            )

            // KPI — honest stats, no fake counters
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VisionKpi("13", "kategori pintar", Modifier.weight(1f))
                VisionKpi("100%", "privasi SAF", Modifier.weight(1f))
                VisionKpi("0", "server kami", Modifier.weight(1f))
            }

            // --- API Key — Liquid Glass Card ---
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(corner = 20.dp, strong = false)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔑  Gemini API Key", fontWeight = FontWeight.SemiBold, fontSize = 13.8.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.55f)
                    ) {
                        Text(
                            " terenkripsi ",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it.trim() },
                    placeholder = { Text("AIza…") },
                    singleLine = true,
                    visualTransformation =
                        if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { keyVisible = !keyVisible }) {
                            Text(
                                if (keyVisible) "Sembunyikan" else "Lihat",
                                fontSize = 11.5.sp,
                                color = VisionColors.accent
                            )
                        }
                    },
                    isError = keyInput.isNotBlank() && !keyInput.startsWith("AIza"),
                    supportingText = {
                        if (keyInput.isNotBlank() && !keyInput.startsWith("AIza")) {
                            Text("API key Gemini biasanya diawali \"AIza\"")
                        }
                    },
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
                            scope.launch { snackbar.showSnackbar("API key tersimpan aman (terenkripsi) ✓") }
                        },
                        enabled = keyInput.isNotBlank() && keyInput != apiKey
                    )
                    if (!apiKey.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                haptics.warning()
                                onClearApiKey()
                                scope.launch { snackbar.showSnackbar("API key dihapus") }
                            },
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.38f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Hapus", fontWeight = FontWeight.Medium) }
                    }
                }
                TextButton(
                    onClick = { uriHandler.openUri(AI_STUDIO_URL) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Belum punya key? Gratis di Google AI Studio ↗",
                        fontSize = 12.sp,
                        color = VisionColors.cyan
                    )
                }
            }

            // --- Folder picker – CardStack — ruixen.ui ---
            Text(
                "Folder sumber",
                style = MaterialTheme.typography.labelSmall,
                color = VisionColors.muted,
                letterSpacing = 0.8.sp
            )
            VisionCardStack(
                folderLabel = folderLabel,
                picked = treeUri != null,
                onPick = onPickFolder,
                onClear = onClearFolder
            )

            Spacer(Modifier.height(4.dp))

            // --- CTA — Apple Tahoe Liquid Glass Button ---
            LiquidGlassButton(
                text = "Mulai Scan & Analisis AI →",
                dark = true,
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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
                "Output → AI_Organized/  •  Maks 150 file per scan  •  Uji coba dulu",
                style = MaterialTheme.typography.bodySmall,
                color = VisionColors.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(48.dp))
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
            .visionAurora()   // aurora drawn FIRST (behind liquid glass)
            .liquidGlass(corner = 28.dp, strong = true)
            .pointerInput(Unit) {
                detectDragGestures { _, drag ->
                    ry = (ry + drag.x * 0.045f).coerceIn(-14f, 14f)
                    rx = (rx - drag.y * 0.045f).coerceIn(-10f, 10f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // floating glass chips
        GlassChip("PDF", Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = 22.dp))
        GlassChip("OCR", Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 30.dp))
        GlassChip("Gemini", Modifier.align(Alignment.BottomStart).offset(x = 36.dp, y = (-22).dp))

        // liquid orb
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
                            listOf(Color.White.copy(alpha = 0.92f), Color.Transparent)
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
            Text(
                "SAF • Scoped Storage",
                style = MaterialTheme.typography.labelSmall,
                color = VisionColors.muted
            )
            Text(
                "Gemini Flash",
                style = MaterialTheme.typography.labelSmall,
                color = VisionColors.muted
            )
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
private fun VisionCardStack(
    folderLabel: String?,
    picked: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val haptics = rememberVisionHaptics()

    Box(Modifier.fillMaxWidth().height(120.dp)) {
        val items = listOf(
            Triple("🔒 /Android, obb, data", "otomatis dilewati demi keamanan", 2),
            Triple("🗂 AI_Organized/", "hasil scan tidak discan ulang", 1),
            Triple(
                if (picked) "📂 ${folderLabel ?: "(folder terpilih)"}" else "📂 Pilih folder…",
                if (picked) "tersimpan — otomatis dipakai lagi" else "SAF • akses terbatas",
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
                        translationY = s.y + 4f
                        rotationZ = s.rot
                        alpha = s.alpha
                    }
                    .liquidGlass(corner = 18.dp, strong = idx == 0)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.3.sp,
                        color = if (idx == 0) VisionColors.ink else VisionColors.muted,
                        maxLines = 1
                    )
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (idx == 0) VisionColors.muted else VisionColors.muted.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { haptics.tick(); onPick() },
            modifier = Modifier.weight(1f),
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
            Text(
                if (picked) "Ganti Folder" else "Pilih Folder di Storage / SD Card",
                fontWeight = FontWeight.Medium
            )
        }
        if (picked) {
            OutlinedButton(
                onClick = { haptics.warning(); onClear() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.38f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Lepas", fontWeight = FontWeight.Medium) }
        }
    }
    Text(
        "App HANYA mengakses folder yang kamu pilih. File tidak dikirim ke server kami — kategori dianalisis langsung oleh Gemini dengan API key milikmu.",
        style = MaterialTheme.typography.bodySmall,
        color = VisionColors.muted,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * Liquid Glass Button – Apple Tahoe.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dark: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.liquidGlassButton(dark = dark),
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
