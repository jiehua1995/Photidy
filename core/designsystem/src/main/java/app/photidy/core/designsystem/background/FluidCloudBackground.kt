package app.photidy.core.designsystem.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.photidy.core.designsystem.theme.PhotidyTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A slow, low-contrast light field used by the Fluid Cloud visual style.
 * It is deliberately cheap: three soft radial gradients, no real-time blur.
 */
@Composable
fun FluidCloudBackground(
    modifier: Modifier = Modifier,
    intensityOverride: Float? = null,
) {
    val colors = PhotidyTheme.colors
    val intensity = (intensityOverride ?: PhotidyTheme.effectIntensity).coerceIn(0f, 1f)
    val animated = PhotidyTheme.animationsEnabled

    val phaseA = cloudDrift(animated, 38_000, 0.0f)
    val phaseB = cloudDrift(animated, 52_000, 0.35f)
    val phaseC = cloudDrift(animated, 64_000, 0.7f)

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.background, colors.backgroundSecondary),
                startY = 0f,
                endY = size.height,
            ),
        )
        if (intensity <= 0.01f) return@Canvas

        val maxDim = max(size.width, size.height)
        val alpha = 0.5f * intensity

        val centerA = Offset(
            x = size.width * (0.28f + 0.14f * sin(phaseA * 2f * Math.PI.toFloat())),
            y = size.height * (0.26f + 0.06f * cos(phaseA * 2f * Math.PI.toFloat())),
        )
        val centerB = Offset(
            x = size.width * (0.74f + 0.12f * cos(phaseB * 2f * Math.PI.toFloat())),
            y = size.height * (0.52f + 0.09f * sin(phaseB * 2f * Math.PI.toFloat())),
        )
        val centerC = Offset(
            x = size.width * (0.42f + 0.18f * sin(phaseC * 2f * Math.PI.toFloat())),
            y = size.height * (0.86f + 0.05f * cos(phaseC * 2f * Math.PI.toFloat())),
        )

        drawSoftOrb(centerA, maxDim * 0.62f, colors.cloudPrimary, alpha)
        drawSoftOrb(centerB, maxDim * 0.55f, colors.cloudSecondary, alpha * 0.9f)
        drawSoftOrb(centerC, maxDim * 0.5f, colors.cloudTertiary, alpha * 0.8f)

        // A barely visible top veil keeps content readable.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.background.copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.35f,
            ),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSoftOrb(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

@Composable
private fun cloudDrift(enabled: Boolean, durationMillis: Int, initial: Float): Float {
    if (!enabled) return initial + 0.15f
    val transition = rememberInfiniteTransition(label = "cloud-$durationMillis")
    val value by transition.animateFloat(
        initialValue = initial,
        targetValue = initial + 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloud-phase-$durationMillis",
    )
    return value
}
