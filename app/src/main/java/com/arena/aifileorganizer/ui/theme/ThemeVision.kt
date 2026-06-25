package com.arena.aifileorganizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arena.aifileorganizer.R

/**
 * AI File Organizer v1.1 – visionOS Liquid Glass
 *
 * Refs 21st.dev:
 * - Apple Tahoe Liquid Glass Button – easemize – 637★
 *   npx shadcn@latest add https://21st.dev/r/easemize/apple-tahoe-liquid-glass-button
 * - Liquid Glass Card – designali-in / aliimam
 *   npx shadcn@latest add https://21st.dev/r/designali-in/liquid-glass-card
 * - Liquid Glass – suraj-xd – 145★
 *   npx shadcn@latest add https://21st.dev/r/suraj-xd/liquid-glass
 * - Animated AI Chat (glass-morphism) – jatin-yadav05
 *   npx shadcn@latest add https://21st.dev/r/jatin-yadav05/animated-ai-chat
 * - SplineScene – serafim
 *   npx shadcn@latest add https://21st.dev/r/serafimcloud/splite
 */

// visionOS warm paper
private val VisionPaper = Color(0xFFEBE7E2)
private val VisionPaper2 = Color(0xFFDCD8D2)
private val VisionInk = Color(0xFF1B1B1D)
private val VisionMuted = Color(0xFF6B6A6D)
private val VisionGlass = Color.White.copy(alpha = 0.44f)
private val VisionGlassStrong = Color.White.copy(alpha = 0.60f)
private val VisionStroke = Color.White.copy(alpha = 0.72f)
private val VisionAccent = Color(0xFF6B5A9A) // violet muted
private val VisionCyan = Color(0xFF4FAEBB)
private val VisionDark = Color(0xFF26232E)

// Light – visionOS default (warm)
private val VisionLight = lightColorScheme(
    primary = VisionInk,
    onPrimary = Color.White,
    primaryContainer = VisionGlassStrong,
    onPrimaryContainer = VisionInk,
    secondary = Color(0xFF5E5C63),
    tertiary = VisionAccent,
    background = VisionPaper,
    onBackground = VisionInk,
    surface = Color.White.copy(alpha = 0.52f),
    onSurface = VisionInk,
    surfaceVariant = Color(0xFFF4F0EA),
    outline = Color(0xFFD5D0C8),
    outlineVariant = VisionStroke,
    scrim = Color(0xFF3A2F28).copy(alpha = 0.10f)
)

// Dark – for users who prefer dark (subtle)
private val VisionDarkScheme = darkColorScheme(
    primary = Color(0xFFE8E4F8),
    onPrimary = Color(0xFF1B1B1D),
    secondary = Color(0xFFA9A7B0),
    tertiary = Color(0xFFB99CFF),
    background = Color(0xFF141218),
    onBackground = Color(0xFFEAE6F0),
    surface = Color.White.copy(alpha = 0.08f),
    onSurface = Color(0xFFEAE6F0),
    surfaceVariant = Color(0xFF23202A),
    outline = Color(0xFF3A3840),
    outlineVariant = Color.White.copy(alpha = 0.12f)
)

@Composable
fun AIFileOrganizerThemeVision(
    darkTheme: Boolean = false, // visionOS default = light warm
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) VisionDarkScheme else VisionLight
    MaterialTheme(
        colorScheme = scheme,
        typography = VisionTypography,
        content = content
    )
}

// visionOS Fonts — downloaded from Google Fonts, bundled in res/font/
private val Fraunces = FontFamily(
    Font(R.font.fraunces, FontWeight.Medium)
)
private val Instrument = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold)
)

val VisionTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 39.sp,
        letterSpacing = (-1.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 12.3.sp,
        lineHeight = 18.sp,
        color = VisionMuted
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 0.2.sp
    )
)

// visionOS color helpers
object VisionColors {
    val paper = VisionPaper
    val paper2 = VisionPaper2
    val ink = VisionInk
    val muted = VisionMuted
    val glass = VisionGlass
    val glassStrong = VisionGlassStrong
    val stroke = VisionStroke
    val accent = VisionAccent
    val cyan = VisionCyan
    val dark = VisionDark
    val success = Color(0xFF3A7A4A)
    val ocr = Color(0xFF2D6F96)
}
