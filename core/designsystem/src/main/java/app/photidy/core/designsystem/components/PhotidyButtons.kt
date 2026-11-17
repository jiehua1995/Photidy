package app.photidy.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import app.photidy.core.designsystem.theme.PhotidySizes
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme

/** Apple-like press feedback: a quick, shallow scale that respects the animation setting. */
@Composable
private fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val animations = PhotidyTheme.animationsEnabled
    val scale by animateFloatAsState(
        targetValue = if (pressed && animations) pressedScale else 1f,
        animationSpec = tween(durationMillis = if (animations) 120 else 0),
        label = "press-scale",
    )
    return scale
}

@Composable
fun PhotidyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = PhotidyTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, pressedScale = 0.96f)
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.accent.copy(alpha = 0.32f),
            disabledContentColor = colors.onAccent.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(
            horizontal = PhotidySpacing.xxl,
            vertical = PhotidySpacing.md,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(PhotidySizes.iconSmall))
            Spacer(Modifier.width(PhotidySpacing.sm))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PhotidySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = PhotidyTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, pressedScale = 0.96f)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, colors.glassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.glass(0.75f),
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(
            horizontal = PhotidySpacing.xxl,
            vertical = PhotidySpacing.md,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(PhotidySizes.iconSmall))
            Spacer(Modifier.width(PhotidySpacing.sm))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PhotidyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = PhotidyTheme.colors.textSecondary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, pressedScale = 0.97f)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.textButtonColors(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.4f),
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PhotidyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = PhotidyTheme.colors.textPrimary,
    container: Boolean = false,
) {
    val colors = PhotidyTheme.colors
    val animations = PhotidyTheme.animationsEnabled
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animations) 0.9f else 1f,
        animationSpec = tween(durationMillis = if (animations) 110 else 0),
        label = "icon-press",
    )
    val background by animateColorAsState(
        targetValue = when {
            container -> colors.glassSurface
            pressed -> colors.accentSoft.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = if (animations) 110 else 0),
        label = "icon-background",
    )
    Box(
        modifier = modifier
            .size(PhotidySizes.touchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                color = background,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(PhotidySizes.icon),
        )
    }
}
