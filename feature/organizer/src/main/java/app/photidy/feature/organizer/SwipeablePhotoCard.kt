package app.photidy.feature.organizer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.theme.PhotidySizes
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.SwipeDirection
import app.photidy.core.model.VisualStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun PhotoCardStack(
    current: PhotoWithState,
    next: PhotoWithState?,
    bindings: GestureBindings,
    onOpenDetail: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    onDragDirectionChanged: (SwipeDirection?) -> Unit = {},
) {
    val colors = PhotidyTheme.colors
    val locale: Locale = LocalConfiguration.current.locales[0]
    BoxWithConstraints(modifier = modifier) {
        val ratio = current.photo.aspectRatio.coerceAtLeast(0.4f)
        val availableWidth = minOf(maxWidth, 640.dp)
        val widthByHeight = maxHeight * ratio
        val cardWidth = minOf(availableWidth, widthByHeight)
        val cardHeight = cardWidth / ratio
        val corner = RoundedCornerShape(PhotidySizes.photoCorner)

        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .align(Alignment.Center),
        ) {
            if (next != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(6.dp)
                        .graphicsLayer {
                            scaleX = 0.95f
                            scaleY = 0.95f
                            alpha = 0.65f
                        }
                        .shadow(10.dp, corner, clip = false),
                ) {
                    PhotoCardFace(
                        item = next,
                        locale = locale,
                        onOpenDetail = {},
                        modifier = Modifier
                            .matchParentSize()
                            .clip(corner)
                            .background(colors.surface),
                    )
                }
            }

            SwipeablePhotoCard(
                item = current,
                bindings = bindings,
                locale = locale,
                onOpenDetail = onOpenDetail,
                onSwipe = onSwipe,
                onDragDirectionChanged = onDragDirectionChanged,
                modifier = Modifier
                    .matchParentSize()
                    .shadow(16.dp, corner, clip = false),
            )
        }
    }
}

/**
 * Gesture and render pipeline:
 * - drag offset lives in snapshot state but is only read inside [graphicsLayer]/draw lambdas,
 *   so dragging does not recompose the card (important on low-end GPUs).
 * - direction changes are the only per-gesture recompositions (overlay content).
 * - release animations run in a composition coroutine and mutate an [Animatable] read in draw.
 */
@Composable
private fun SwipeablePhotoCard(
    item: PhotoWithState,
    bindings: GestureBindings,
    locale: Locale,
    onOpenDetail: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onDragDirectionChanged: (SwipeDirection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val motion = PhotidyTheme.motion
    val animations = PhotidyTheme.animationsEnabled
    val intensity = PhotidyTheme.effectIntensity
    val style = PhotidyTheme.visualStyle
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val corner = RoundedCornerShape(PhotidySizes.photoCorner)

    val dragOffset = remember(item.photo.id) { mutableStateOf(Offset.Zero) }
    val animatedOffset = remember(item.photo.id) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    var dragging by remember(item.photo.id) { mutableStateOf(false) }
    var dragDirection by remember(item.photo.id) { mutableStateOf<SwipeDirection?>(null) }
    var animationJob by remember(item.photo.id) { mutableStateOf<Job?>(null) }
    var committed by remember(item.photo.id) { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    val currentOffset: () -> Offset = {
        if (dragging) dragOffset.value else animatedOffset.value
    }
    val currentProgress: () -> Float = {
        val direction = dragDirection
        if (direction == null) {
            0f
        } else {
            val distance = directionalDistance(currentOffset(), direction)
            (distance / thresholdFor(direction, widthPx, heightPx)).coerceIn(0f, 1f)
        }
    }

    val action = dragDirection?.let { bindings.actionFor(it) } ?: GestureAction.NONE
    val edgeGuard = with(density) { 26.dp.toPx() }
    val flingThreshold = with(density) { 1_050.dp.toPx() }
    val springSpec = spring<Offset>(
        dampingRatio = motion.springGentle.dampingRatio,
        stiffness = motion.springGentle.stiffness,
    )

    Box(
        modifier = modifier
            .testTag("photo_card")
            .onSizeChanged {
                widthPx = it.width.toFloat()
                heightPx = it.height.toFloat()
            }
            .pointerInput(item.photo.id, bindings, animations) {
                var ignoreGesture = false
                var tracker: VelocityTracker? = null

                detectDragGestures(
                    onDragStart = { start ->
                        ignoreGesture = committed ||
                            start.x < edgeGuard ||
                            start.x > size.width - edgeGuard
                        if (!ignoreGesture) {
                            animationJob?.cancel()
                            animationJob = null
                            dragOffset.value = Offset.Zero
                            dragging = true
                            dragDirection = null
                        }
                    },
                    onDrag = drag@{ change, delta ->
                        if (ignoreGesture) return@drag
                        tracker?.addPosition(change.uptimeMillis, change.position)
                        dragOffset.value += delta
                        val newDirection = directionOf(dragOffset.value)
                        if (newDirection != dragDirection) {
                            dragDirection = newDirection
                            onDragDirectionChanged(newDirection)
                        }
                        change.consume()
                    },
                    onDragEnd = end@{
                        if (ignoreGesture) {
                            ignoreGesture = false
                            return@end
                        }
                        val velocity = tracker?.calculateVelocity() ?: Velocity.Zero
                        val releaseDirection = directionOf(dragOffset.value) ?: dragDirection
                        val releaseAction = releaseDirection
                            ?.let { bindings.actionFor(it) }
                            ?: GestureAction.NONE
                        val distance = releaseDirection
                            ?.let { directionalDistance(dragOffset.value, it) }
                            ?: 0f
                        val threshold = releaseDirection
                            ?.let { thresholdFor(it, widthPx, heightPx) }
                            ?: Float.MAX_VALUE
                        val speed = releaseDirection
                            ?.let { directionalSpeed(velocity.x, velocity.y, it) }
                            ?: 0f
                        val shouldCommit = releaseDirection != null &&
                            releaseAction != GestureAction.NONE &&
                            (distance > threshold || speed > flingThreshold)
                        val commitDirection = releaseDirection.takeIf { shouldCommit }
                        val startOffset = dragOffset.value

                        animationJob = scope.launch {
                            animatedOffset.snapTo(startOffset)
                            dragging = false

                            if (commitDirection != null) {
                                if (releaseAction.requiresAlbum) {
                                    // Give the card back while the album picker is open.
                                    animatedOffset.animateTo(Offset.Zero, springSpec)
                                    dragDirection = null
                                    onDragDirectionChanged(null)
                                    onSwipe(commitDirection)
                                } else {
                                    committed = true
                                    val exit = exitOffset(
                                        commitDirection,
                                        widthPx,
                                        heightPx,
                                        startOffset,
                                    )
                                    if (animations) {
                                        animatedOffset.animateTo(
                                            targetValue = exit,
                                            animationSpec = tween(
                                                durationMillis = 260,
                                                easing = motion.easingStandard,
                                            ),
                                        )
                                    } else {
                                        animatedOffset.snapTo(exit)
                                    }
                                    onSwipe(commitDirection)
                                }
                            } else {
                                if (animations) {
                                    animatedOffset.animateTo(Offset.Zero, springSpec)
                                } else {
                                    animatedOffset.snapTo(Offset.Zero)
                                }
                                dragDirection = null
                                onDragDirectionChanged(null)
                            }
                        }
                    },
                    onDragCancel = {
                        val startOffset = dragOffset.value
                        animationJob?.cancel()
                        animationJob = scope.launch {
                            animatedOffset.snapTo(startOffset)
                            dragging = false
                            if (animations) {
                                animatedOffset.animateTo(Offset.Zero, springSpec)
                            } else {
                                animatedOffset.snapTo(Offset.Zero)
                            }
                            dragDirection = null
                            onDragDirectionChanged(null)
                        }
                    },
                )
            },
    ) {
        if (style == VisualStyle.FLUID_CLOUD &&
            action != GestureAction.NONE &&
            intensity > 0.02f
        ) {
            val glowColor = actionColor(action, colors)
            Canvas(modifier = Modifier.matchParentSize()) {
                val progress = currentProgress()
                if (progress <= 0.04f) return@Canvas
                val offset = currentOffset()
                val center = Offset(
                    x = size.width / 2f - offset.x * 0.25f,
                    y = size.height / 2f - offset.y * 0.25f,
                )
                val radius = max(size.width, size.height) * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.34f * progress * intensity),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val offset = currentOffset()
                    translationX = offset.x
                    translationY = offset.y
                    val progress = currentProgress()
                    rotationZ = (offset.x / widthPx.coerceAtLeast(1f)) * 8f
                    scaleX = 1f - progress * 0.015f
                    scaleY = 1f - progress * 0.015f
                },
        ) {
            PhotoCardFace(
                item = item,
                locale = locale,
                onOpenDetail = onOpenDetail,
                modifier = Modifier
                    .matchParentSize()
                    .clip(corner)
                    .background(colors.surface),
            )
            if (action != GestureAction.NONE) {
                Box(
                    modifier = Modifier
                        .align(feedbackAlignment(dragDirection))
                        .padding(
                            start = PhotidySpacing.md,
                            end = PhotidySpacing.md,
                            top = PhotidySpacing.md,
                            bottom = PhotidySpacing.md,
                        )
                        .graphicsLayer {
                            val progress = currentProgress()
                            alpha = progress
                            scaleX = 0.88f + 0.12f * progress
                            scaleY = 0.88f + 0.12f * progress
                        },
                ) {
                    ActionFeedback(action = action)
                }
            }
        }

        if (action != GestureAction.NONE) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(corner)
                    .graphicsLayer { alpha = 0.55f * currentProgress() }
                    .border(
                        width = 2.dp,
                        color = actionColor(action, colors),
                        shape = corner,
                    ),
            )
        }
    }
}

@Composable
private fun PhotoCardFace(
    item: PhotoWithState,
    locale: Locale,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val photo = item.photo
    val duration = photo.durationSeconds
    Box(modifier = modifier) {
        PhotoImage(
            photo = photo,
            modifier = Modifier.matchParentSize(),
            contentDescription = photo.name,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(128.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.scrim.copy(alpha = 0.62f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PhotidySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = PhotidyFormatters.formatDate(photo.capturedAt, locale),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.94f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm)) {
                Text(
                    text = PhotidyFormatters.formatBytes(photo.sizeBytes, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                if (photo.isVideo && duration != null) {
                    Text(
                        text = PhotidyFormatters.formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(PhotidySpacing.md)
                .clip(CircleShape)
                .background(colors.scrim.copy(alpha = 0.32f))
                .clickable(onClick = onOpenDetail)
                .padding(horizontal = PhotidySpacing.sm, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = stringResource(LocR.string.organizer_card_detail),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        if (item.state.favorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PhotidySpacing.md)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.scrim.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(LocR.string.cd_favorite),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionFeedback(
    action: GestureAction,
    modifier: Modifier = Modifier,
) {
    val colors = PhotidyTheme.colors
    val color = actionColor(action, colors)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(colors.scrim.copy(alpha = 0.55f))
            .padding(horizontal = PhotidySpacing.lg, vertical = PhotidySpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.24f))
                .border(2.dp, color.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = actionIcon(action),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(42.dp),
            )
        }
        Text(
            text = stringResource(actionLabelRes(action)),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun feedbackAlignment(direction: SwipeDirection?): Alignment = when (direction) {
    // The card moves away from the finger, so the badge sits on the side that stays visible.
    SwipeDirection.LEFT -> Alignment.CenterEnd
    SwipeDirection.RIGHT -> Alignment.CenterStart
    SwipeDirection.UP -> Alignment.BottomCenter
    SwipeDirection.DOWN -> Alignment.TopCenter
    null -> Alignment.Center
}

internal fun actionIcon(action: GestureAction): ImageVector = when (action) {
    GestureAction.ADD_TO_DELETE_LIST -> Icons.Filled.Delete
    GestureAction.KEEP -> Icons.Filled.Check
    GestureAction.FAVORITE -> Icons.Filled.Favorite
    GestureAction.ADD_TO_ALBUM -> Icons.AutoMirrored.Filled.List
    GestureAction.SKIP -> Icons.AutoMirrored.Filled.ArrowForward
    GestureAction.NONE -> Icons.Filled.Close
}

internal fun actionLabelRes(action: GestureAction): Int = when (action) {
    GestureAction.ADD_TO_DELETE_LIST -> LocR.string.organizer_action_delete
    GestureAction.KEEP -> LocR.string.organizer_action_keep
    GestureAction.FAVORITE -> LocR.string.organizer_action_favorite
    GestureAction.ADD_TO_ALBUM -> LocR.string.organizer_action_album
    GestureAction.SKIP -> LocR.string.organizer_action_skip
    GestureAction.NONE -> LocR.string.organizer_action_none
}

internal fun actionColor(
    action: GestureAction,
    colors: app.photidy.core.designsystem.theme.PhotidyColors,
): Color = when (action) {
    GestureAction.ADD_TO_DELETE_LIST -> colors.danger
    GestureAction.KEEP -> colors.success
    GestureAction.FAVORITE -> colors.accent
    GestureAction.ADD_TO_ALBUM -> colors.accent
    GestureAction.SKIP -> colors.textSecondary
    GestureAction.NONE -> colors.textTertiary
}

private fun directionOf(offset: Offset): SwipeDirection? {
    if (offset.getDistance() < 14f) return null
    return if (abs(offset.x) >= abs(offset.y)) {
        if (offset.x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
    } else {
        if (offset.y > 0) SwipeDirection.DOWN else SwipeDirection.UP
    }
}

private fun directionalDistance(offset: Offset, direction: SwipeDirection): Float = when (direction) {
    SwipeDirection.LEFT -> -offset.x
    SwipeDirection.RIGHT -> offset.x
    SwipeDirection.UP -> -offset.y
    SwipeDirection.DOWN -> offset.y
}

private fun directionalSpeed(vx: Float, vy: Float, direction: SwipeDirection): Float = when (direction) {
    SwipeDirection.LEFT -> -vx
    SwipeDirection.RIGHT -> vx
    SwipeDirection.UP -> -vy
    SwipeDirection.DOWN -> vy
}

private fun thresholdFor(direction: SwipeDirection, width: Float, height: Float): Float =
    if (direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT) {
        width * 0.26f
    } else {
        height * 0.22f
    }

private fun exitOffset(
    direction: SwipeDirection,
    width: Float,
    height: Float,
    current: Offset,
): Offset = when (direction) {
    SwipeDirection.LEFT -> Offset(-width * 1.4f, current.y)
    SwipeDirection.RIGHT -> Offset(width * 1.4f, current.y)
    SwipeDirection.UP -> Offset(current.x, -height * 1.3f)
    SwipeDirection.DOWN -> Offset(current.x, height * 1.3f)
}
