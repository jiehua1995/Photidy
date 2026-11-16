package app.photidy.feature.settings

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.SwipeDirection
import kotlin.math.abs

/**
 * A small, honest sandbox: swipes never commit, they only preview the mapped action,
 * so users can feel the gesture before using it in the organizer.
 */
@Composable
internal fun GestureDemoCard(
    bindings: GestureBindings,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val motion = PhotidyTheme.motion
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var direction by remember { mutableStateOf<SwipeDirection?>(null) }
    var hapticFired by remember { mutableStateOf(false) }

    val animatedOffset by animateOffsetAsState(
        targetValue = if (dragging) dragOffset else Offset.Zero,
        animationSpec = if (dragging) {
            snap()
        } else {
            spring(
                dampingRatio = motion.springGentle.dampingRatio,
                stiffness = motion.springGentle.stiffness,
            )
        },
        label = "gesture-demo-offset",
    )

    val action = direction?.let { bindings.actionFor(it) } ?: GestureAction.NONE
    val rotation = (animatedOffset.x / with(density) { 260.dp.toPx() }) * 6f
    val threshold = with(density) { 42.dp.toPx() }

    Box(
        modifier = modifier
            .height(156.dp)
            .graphicsLayer {
                translationX = animatedOffset.x
                translationY = animatedOffset.y
                rotationZ = rotation
            }
            .pointerInput(bindings, hapticsEnabled) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        hapticFired = false
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffset = Offset.Zero
                        direction = null
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                        direction = null
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        dragOffset += delta
                        direction = demoDirection(dragOffset)
                        if (hapticsEnabled && !hapticFired && dragOffset.getDistance() > threshold) {
                            hapticFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                )
            }
            .border(1.dp, colors.glassBorder, RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.accentSoft.copy(alpha = 0.55f),
                        colors.cloudPrimary.copy(alpha = 0.45f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(PhotidySpacing.md)
                    .size(18.dp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(PhotidySpacing.md)
                    .size(18.dp),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(PhotidySpacing.md)
                    .size(18.dp),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PhotidySpacing.md)
                    .size(18.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = PhotidySpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
            ) {
                if (action == GestureAction.NONE) {
                    Text(
                        text = stringResource(LocR.string.settings_gesture_demo_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
                    ) {
                        Icon(
                            imageVector = demoActionIcon(action),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(
                                LocR.string.settings_gesture_demo_result,
                                stringResource(actionLabelRes(action)),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun demoDirection(offset: Offset): SwipeDirection? {
    if (offset.getDistance() < 12f) return null
    return if (abs(offset.x) >= abs(offset.y)) {
        if (offset.x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
    } else {
        if (offset.y > 0) SwipeDirection.DOWN else SwipeDirection.UP
    }
}

private fun demoActionIcon(action: GestureAction): ImageVector = when (action) {
    GestureAction.ADD_TO_DELETE_LIST -> Icons.Filled.Delete
    GestureAction.KEEP -> Icons.Filled.Check
    GestureAction.FAVORITE -> Icons.Filled.Favorite
    GestureAction.ADD_TO_ALBUM -> Icons.AutoMirrored.Filled.List
    GestureAction.SKIP -> Icons.AutoMirrored.Filled.ArrowForward
    GestureAction.NONE -> Icons.Filled.Close
}
