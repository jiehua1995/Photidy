package app.photidy.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.photidy.core.designsystem.background.FluidCloudBackground
import app.photidy.core.designsystem.theme.PhotidyRadius
import app.photidy.core.designsystem.theme.PhotidySizes
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme
import app.photidy.core.model.VisualStyle

@Composable
fun PhotidyScreen(
    modifier: Modifier = Modifier,
    showCloud: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = PhotidyTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (showCloud) {
            when (PhotidyTheme.visualStyle) {
                VisualStyle.FLUID_CLOUD -> FluidCloudBackground(Modifier.fillMaxSize())
                VisualStyle.LIQUID_GLASS -> GlassBackdrop(Modifier.fillMaxSize())
                VisualStyle.MINIMAL -> Unit
            }
        }
        content()
    }
}

/**
 * Two soft light pools behind the glass surfaces. Without a backdrop the translucent
 * cards read as flat tinted boxes, which makes "Liquid Glass" indistinguishable from Minimal.
 */
@Composable
private fun GlassBackdrop(modifier: Modifier = Modifier) {
    val colors = PhotidyTheme.colors
    val intensity = PhotidyTheme.effectIntensity
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.background, colors.backgroundSecondary),
            ),
        )
        val radius = size.width * 0.9f
        val alpha = 0.18f + 0.30f * intensity

        val warm = Offset(size.width * 0.16f, size.height * 0.10f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.accentSoft.copy(alpha = alpha), Color.Transparent),
                center = warm,
                radius = radius,
            ),
            radius = radius,
            center = warm,
        )

        val cool = Offset(size.width * 0.88f, size.height * 0.82f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.cloudSecondary.copy(alpha = alpha * 0.85f), Color.Transparent),
                center = cool,
                radius = radius,
            ),
            radius = radius,
            center = cool,
        )
    }
}

/**
 * The core surface primitive: solid in Minimal, softly translucent in Liquid Glass,
 * lightly tinted in Fluid Cloud. Effect intensity scales tint and elevation.
 */
@Composable
fun PhotidySurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PhotidyRadius.lg),
    contentPadding: PaddingValues = PaddingValues(PhotidySpacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PhotidyTheme.colors
    val style = PhotidyTheme.visualStyle
    val intensity = PhotidyTheme.effectIntensity

    val fill: Brush = when (style) {
        VisualStyle.MINIMAL -> SolidColor(colors.surface)
        VisualStyle.LIQUID_GLASS -> Brush.linearGradient(
            colors = listOf(
                colors.glass(1.35f + 0.45f * intensity),
                colors.glass(0.55f + 0.30f * intensity),
            ),
        )

        VisualStyle.FLUID_CLOUD -> Brush.linearGradient(
            colors = listOf(
                colors.glass(1.05f + 0.30f * intensity),
                colors.accentSoft.copy(alpha = 0.14f + 0.16f * intensity),
            ),
        )
    }

    val elevation = when (style) {
        VisualStyle.MINIMAL -> 0.dp
        VisualStyle.LIQUID_GLASS -> 4.dp + 4.dp * intensity
        VisualStyle.FLUID_CLOUD -> 2.dp + 5.dp * intensity
    }
    val borderColor = colors.glassBorder

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = colors.shadow.copy(alpha = 0.22f),
                spotColor = colors.shadow.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, borderColor, shape)
            .drawWithContent {
                drawContent()
                if (style != VisualStyle.MINIMAL) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.10f + 0.16f * intensity),
                        start = Offset(0f, 0.6f),
                        end = Offset(size.width, 0.6f),
                        strokeWidth = 1f,
                    )
                }
            }
            .then(clickableModifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        content = content,
    )
}

@Composable
fun PhotidyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = PhotidyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PhotidySizes.topBarHeight)
            .padding(horizontal = PhotidySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PhotidyIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.width(PhotidySpacing.md))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = PhotidySpacing.xs, end = PhotidySpacing.sm),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
        Spacer(Modifier.width(PhotidySpacing.xs))
    }
}

@Composable
fun PhotidySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = PhotidyTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xxs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun PhotidyEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = PhotidyTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PhotidySpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(PhotidySizes.iconLarge),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(PhotidySpacing.xs))
            PhotidyPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun PhotidyMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    PhotidySurface(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = PhotidySpacing.lg,
            vertical = PhotidySpacing.md,
        ),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

enum class PhotidyBadgeTone {
    NEUTRAL,
    ACCENT,
    DANGER,
}

@Composable
fun PhotidyBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: PhotidyBadgeTone = PhotidyBadgeTone.NEUTRAL,
) {
    val colors = PhotidyTheme.colors
    val background = when (tone) {
        PhotidyBadgeTone.NEUTRAL -> colors.backgroundSecondary
        PhotidyBadgeTone.ACCENT -> colors.accentSoft
        PhotidyBadgeTone.DANGER -> colors.dangerSoft
    }
    val contentColor = when (tone) {
        PhotidyBadgeTone.NEUTRAL -> colors.textSecondary
        PhotidyBadgeTone.ACCENT -> colors.accent
        PhotidyBadgeTone.DANGER -> colors.danger
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background, CircleShape)
            .padding(horizontal = PhotidySpacing.sm, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
fun PhotidyProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val animations = PhotidyTheme.animationsEnabled
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animations) {
            tween(durationMillis = 520)
        } else {
            tween(durationMillis = 0)
        },
        label = "progress-line",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(colors.glassBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(colors.accent),
        )
    }
}

@Composable
fun PhotidyConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = PhotidyTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(PhotidyRadius.xl),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (destructive) colors.danger else colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        },
    )
}
