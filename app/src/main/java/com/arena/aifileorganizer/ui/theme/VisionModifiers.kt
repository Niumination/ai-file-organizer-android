package com.arena.aifileorganizer.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * visionOS Liquid Glass modifiers
 * Port of 21st.dev components:
 * - Apple Tahoe Liquid Glass Button (easemize)
 * - Liquid Glass Card (designali-in)
 * - Liquid Glass (suraj-xd)
 * - Animated AI Chat glass-morphism (jatin-yadav05)
 *
 * Note: Android Compose doesn't support native background blur.
 * The glass effect is achieved via translucent gradient overlays
 * that let the paper background (#EBE7E2) show through — no blur needed.
 */

// --- Liquid Glass Card ---
// designali-in / liquid-glass-card
fun Modifier.visionGlass(
    corner: Dp = 20.dp,
    alpha: Float = 0.44f,
    strong: Boolean = false
) = composed {
    val bgAlpha = if (strong) 0.60f else alpha
    this
        .clip(RoundedCornerShape(corner))
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = bgAlpha + 0.04f),
                    Color.White.copy(alpha = bgAlpha)
                )
            )
        )
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.82f),
                    Color.White.copy(alpha = 0.36f)
                )
            ),
            RoundedCornerShape(corner)
        )
        // specular highlight top
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.52f),
                    0.34f to Color.White.copy(alpha = 0.06f),
                    1f to Color.Transparent
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx())
            )
        }
}

// Frosted glass surface – translucent, shows paper background
fun Modifier.liquidGlass(
    corner: Dp = 20.dp,
    strong: Boolean = false
) = this
    .clip(RoundedCornerShape(corner))
    .drawBehind {
        // translucent glass layer — paper bg shows through
        val bgAlpha = if (strong) 0.60f else 0.46f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = bgAlpha + 0.03f),
                    Color.White.copy(alpha = bgAlpha - 0.03f)
                )
            ),
            cornerRadius = CornerRadius(corner.toPx())
        )
    }
    .border(
        1.dp,
        Color.White.copy(alpha = 0.74f),
        RoundedCornerShape(corner)
    )
    .drawWithContent {
        drawContent()
        // top specular highlight
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = size.height * 0.42f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx())
        )
    }

// --- Apple Tahoe Liquid Glass Button ---
// easemize / apple-tahoe-liquid-glass-button
fun Modifier.liquidGlassButton(
    enabled: Boolean = true,
    dark: Boolean = false
) = composed {
    val bgBrush = if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF2B2933), Color(0xFF1E1C24))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.52f))
        )
    }
    this
        .clip(RoundedCornerShape(16.dp))
        .background(bgBrush)
        .border(
            1.dp,
            if (dark) Color.White.copy(alpha = 0.12f)
            else Color.White.copy(alpha = 0.82f),
            RoundedCornerShape(16.dp)
        )
        .drawWithContent {
            drawContent()
            // liquid sheen top
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.14f else 0.72f),
                        Color.Transparent
                    ),
                    endY = size.height * 0.5f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )
        }
}

// --- Spatial aurora background ---
// SplineScene – serafim
fun Modifier.visionAurora() = drawBehind {
    // violet spot top-right — higher alpha so visible through glass
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFC2ACFF).copy(alpha = 0.45f), Color.Transparent),
            center = Offset(size.width * 0.74f, size.height * 0.28f),
            radius = 320f
        ),
        radius = 320f,
        center = Offset(size.width * 0.74f, size.height * 0.28f)
    )
    // cyan spot bottom-left
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF8ADFFF).copy(alpha = 0.35f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.78f),
            radius = 260f
        ),
        radius = 260f,
        center = Offset(size.width * 0.18f, size.height * 0.78f)
    )
    // mint spot bottom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFCCE9A0).copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 1.05f),
            radius = 340f
        ),
        radius = 340f,
        center = Offset(size.width * 0.5f, size.height * 1.05f)
    )
}

// --- 3D tilt — Interactive 3D Character / 3D Folder ---
// dhiluxui / avanishverma4
fun Modifier.visionTilt(rx: Float, ry: Float) =
    this.graphicsLayer {
        rotationX = rx
        rotationY = ry
        cameraDistance = 14 * density
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
    }

// --- Bob / breathe ---
// Animated AI Chat – jatin-yadav05
@Composable
fun rememberVisionBob(): Float {
    val inf = rememberInfiniteTransition(label = "visionBob")
    return inf.animateFloat(
        initialValue = -5.5f, targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "bob"
    ).value
}

@Composable
fun rememberVisionBreathe(): Float {
    val inf = rememberInfiniteTransition(label = "breathe")
    return inf.animateFloat(
        initialValue = 0.998f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "breathe"
    ).value
}

// --- Liquid sheen sweep ---
// Apple Tahoe Liquid Glass Button
@Composable
fun rememberLiquidSheen(hover: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (hover) 1.35f else -0.35f,
        animationSpec = tween(if (hover) 520 else 0, easing = FastOutSlowInEasing),
        label = "sheen"
    ).value
}

// --- Press spring ---
// shadcn / FM spring
fun Modifier.visionPress(onClick: () -> Unit) = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "press",
        finishedListener = { if (pressed) { pressed = false } }
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            pressed = true
            onClick()
        }
}

// --- CardStack positions ---
// ruixen.ui / card-stack
data class VisionStackSpec(
    val scale: Float, val y: Float, val rot: Float, val alpha: Float
)
fun visionStackAt(i: Int) = when (i) {
    0 -> VisionStackSpec(1f, 0f, 0f, 1f)
    1 -> VisionStackSpec(0.982f, -10f, 1.4f, 0.94f)
    2 -> VisionStackSpec(0.965f, -20f, -2.4f, 0.80f)
    else -> VisionStackSpec(0.93f, -30f, 0f, 0f)
}
