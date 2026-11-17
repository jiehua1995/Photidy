package app.photidy.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.photidy.core.designsystem.theme.PhotidyRadius
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme

data class PhotidySegment<T>(
    val value: T,
    val label: String,
)

@Composable
fun <T> PhotidySegmentedControl(
    options: List<PhotidySegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val animations = PhotidyTheme.animationsEnabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.backgroundSecondary.copy(alpha = 0.65f), CircleShape)
            .border(1.dp, colors.glassBorder, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent,
                animationSpec = tween(durationMillis = if (animations) 200 else 0),
                label = "segment-background",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clip(CircleShape)
                    .background(background, CircleShape)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option.value) },
                    )
                    .padding(horizontal = PhotidySpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun PhotidyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = PhotidyTheme.colors
    val animations = PhotidyTheme.animationsEnabled
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && animations) 0.96f else 1f,
        animationSpec = tween(durationMillis = if (animations) 110 else 0),
        label = "chip-press",
    )
    val background by animateColorAsState(
        targetValue = when {
            !enabled -> colors.surface.copy(alpha = 0.18f)
            selected -> colors.accentSoft
            else -> colors.surface.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = if (animations) 180 else 0),
        label = "chip-background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected && enabled) {
            colors.accent.copy(alpha = 0.35f)
        } else {
            colors.glassBorder
        },
        animationSpec = tween(durationMillis = if (animations) 180 else 0),
        label = "chip-border",
    )
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(horizontal = PhotidySpacing.lg, vertical = PhotidySpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> colors.textTertiary
                selected -> colors.textPrimary
                else -> colors.textSecondary
            },
            maxLines = 1,
        )
    }
}

@Composable
fun PhotidySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.glassBorder,
            activeTickColor = colors.onAccent,
            inactiveTickColor = colors.textTertiary,
        ),
    )
}

@Composable
fun PhotidySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = PhotidyTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = colors.textTertiary,
            uncheckedTrackColor = colors.backgroundSecondary,
            uncheckedBorderColor = colors.glassBorder,
        ),
    )
}

@Composable
fun PhotidySettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = PhotidyTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.sm))
            .then(clickModifier)
            .padding(vertical = PhotidySpacing.sm)
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        trailing()
    }
}

@Composable
fun PhotidyDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PhotidyTheme.colors.glassBorder),
    )
}
