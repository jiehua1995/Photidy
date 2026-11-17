package app.photidy.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.photidy.core.designsystem.theme.PhotidyTheme

/** One destination in the floating bottom bar. */
data class PhotidyTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating glass tab bar. Selection fades a soft pill in and grows the icon slightly, which
 * mirrors the restrained motion used by the rest of the app.
 */
@Composable
fun PhotidyBottomBar(
    items: List<PhotidyTabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val animations = PhotidyTheme.animationsEnabled
    PhotidySurface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                val background by animateColorAsState(
                    targetValue = if (selected) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = tween(durationMillis = if (animations) 220 else 0),
                    label = "tab-background",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) colors.accent else colors.textTertiary,
                    animationSpec = tween(durationMillis = if (animations) 220 else 0),
                    label = "tab-content",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1f,
                    animationSpec = tween(durationMillis = if (animations) 220 else 0),
                    label = "tab-icon",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .clip(CircleShape)
                        .background(background)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onSelect(item.route) },
                        )
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
