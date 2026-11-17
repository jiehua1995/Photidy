package app.photidy.feature.organizer

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyIconButton
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyProgressLine
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySecondaryButton
import app.photidy.core.designsystem.components.PhotidySectionHeader
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.data.AlbumMove
import app.photidy.core.localization.AlbumNames
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.Album
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.PhotoScene
import app.photidy.core.model.PhotoState
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.SwipeDirection
import java.util.Locale

@Composable
fun OrganizerRoute(
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onChoosePhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: OrganizerViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val resources = LocalResources.current
    val undoneMessage = stringResource(LocR.string.organizer_undo_done)
    val moveFailedMessage = stringResource(LocR.string.organizer_move_failed)
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMove by remember { mutableStateOf<AlbumMove?>(null) }

    val moveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val move = pendingMove
        pendingMove = null
        if (move != null) {
            viewModel.onMoveResult(move, result.resultCode == Activity.RESULT_OK)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OrganizerEvent.Undone -> snackbarHostState.showSnackbar(undoneMessage)
                is OrganizerEvent.RequestMove -> {
                    pendingMove = event.move
                    moveLauncher.launch(
                        IntentSenderRequest.Builder(event.move.intentSender).build(),
                    )
                }
                is OrganizerEvent.MoveDone -> {
                    val albumName = event.albumTitle
                        ?: resources.getString(AlbumNames.nameRes(event.albumId))
                    snackbarHostState.showSnackbar(
                        resources.getString(LocR.string.organizer_move_done, albumName),
                    )
                }
                OrganizerEvent.MoveFailed -> snackbarHostState.showSnackbar(moveFailedMessage)
            }
        }
    }

    fun perform(action: GestureAction) {
        if (action == GestureAction.NONE) return
        if (state.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        viewModel.onAction(action)
    }

    fun performDirection(direction: SwipeDirection) {
        if (state.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        viewModel.onDirection(direction)
    }

    OrganizerScreen(
        state = state,
        onBack = onBack,
        onOpenReview = onOpenReview,
        onOpenPhoto = onOpenPhoto,
        onChoosePhotos = onChoosePhotos,
        onAction = ::perform,
        onDirection = ::performDirection,
        onUndo = viewModel::undo,
        onChooseAlbum = viewModel::chooseAlbum,
        onDismissAlbumPicker = viewModel::dismissAlbumPicker,
        snackbarHostState = snackbarHostState,
        onToggleBatch = viewModel::toggleBatchMode,
        onToggleBatchSelection = viewModel::toggleBatchSelection,
        onSelectAllBatch = viewModel::selectAllBatch,
        onClearBatchSelection = viewModel::clearBatchSelection,
        onBatchAction = viewModel::applyBatch,
        modifier = modifier,
    )
}

@Composable
fun OrganizerScreen(
    state: OrganizerUiState,
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onChoosePhotos: () -> Unit,
    onAction: (GestureAction) -> Unit,
    onDirection: (SwipeDirection) -> Unit,
    onUndo: () -> Unit,
    onChooseAlbum: (String) -> Unit,
    onDismissAlbumPicker: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onToggleBatch: () -> Unit = {},
    onToggleBatchSelection: (String) -> Unit = {},
    onSelectAllBatch: () -> Unit = {},
    onClearBatchSelection: () -> Unit = {},
    onBatchAction: (GestureAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass == PhotidyWidthClass.EXPANDED
    val colors = Theme.colors
    val current = state.current
    var activeDirection by remember { mutableStateOf<SwipeDirection?>(null) }

    PhotidyScreen(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = PhotidySpacing.xs,
                            end = PhotidySpacing.md,
                            top = PhotidySpacing.xs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhotidyIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(LocR.string.cd_back),
                        onClick = onBack,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PhotidySpacing.xs),
                    ) {
                        Text(
                            text = stringResource(LocR.string.organizer_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.hasSession) {
                            Text(
                                text = stringResource(
                                    LocR.string.organizer_progress,
                                    state.cursor,
                                    state.total,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    if (state.hasSession) {
                        PhotidyTextButton(
                            text = stringResource(
                                if (state.batchMode) {
                                    LocR.string.organizer_batch_done
                                } else {
                                    LocR.string.organizer_batch
                                },
                            ),
                            onClick = onToggleBatch,
                            tint = colors.accent,
                        )
                    }
                    if (!state.batchMode) {
                        PhotidyIconButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(LocR.string.cd_change_scope),
                            onClick = onChoosePhotos,
                        )
                        PhotidyIconButton(
                            icon = Icons.Filled.Refresh,
                            contentDescription = stringResource(LocR.string.cd_undo),
                            onClick = onUndo,
                            enabled = state.canUndo,
                        )
                    }
                }

                if (state.hasSession) {
                    PhotidyProgressLine(
                        progress = state.progress,
                        modifier = Modifier.padding(horizontal = PhotidySpacing.xxl),
                    )
                }

                when {
                    state.isLoading -> Spacer(Modifier.weight(1f))

                    !state.hasSession -> Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PhotidyEmptyState(
                            title = stringResource(LocR.string.organizer_empty_title),
                            message = stringResource(LocR.string.organizer_empty_message),
                            actionLabel = stringResource(LocR.string.organizer_empty_action),
                            onAction = onChoosePhotos,
                        )
                    }

                    state.isComplete -> OrganizerCompletion(
                        state = state,
                        onOpenReview = onOpenReview,
                        onBackHome = onBack,
                        onUndo = onUndo,
                        modifier = Modifier.weight(1f),
                    )

                    state.batchMode -> OrganizerBatchArea(
                        state = state,
                        onToggleSelection = onToggleBatchSelection,
                        onSelectAll = onSelectAllBatch,
                        onClearSelection = onClearBatchSelection,
                        onApply = onBatchAction,
                        modifier = Modifier.weight(1f),
                    )

                    current != null -> {
                        if (expanded) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = PhotidySpacing.xxl),
                                horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.lg),
                            ) {
                                OrganizerCardArea(
                                    state = state,
                                    current = current,
                                    activeDirection = activeDirection,
                                    onDragDirectionChanged = { activeDirection = it },
                                    onOpenPhoto = onOpenPhoto,
                                    onDirection = onDirection,
                                    onAction = onAction,
                                    modifier = Modifier.weight(1f),
                                )
                                UpNextPanel(
                                    state = state,
                                    locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0],
                                    modifier = Modifier
                                        .width(300.dp)
                                        .fillMaxHeight(),
                                )
                            }
                        } else {
                            OrganizerCardArea(
                                state = state,
                                current = current,
                                activeDirection = activeDirection,
                                onDragDirectionChanged = { activeDirection = it },
                                onOpenPhoto = onOpenPhoto,
                                onDirection = onDirection,
                                onAction = onAction,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = PhotidySpacing.lg,
                        end = PhotidySpacing.lg,
                        bottom = 112.dp,
                    ),
            ) { data ->
                Snackbar(
                    containerColor = colors.surface,
                    contentColor = colors.textPrimary,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (state.pendingAlbumPhotoId != null) {
        AlbumPickerSheet(
            albums = state.albums,
            onChoose = onChooseAlbum,
            onDismiss = onDismissAlbumPicker,
        )
    }
}

@Composable
private fun OrganizerCardArea(
    state: OrganizerUiState,
    current: PhotoWithState,
    activeDirection: SwipeDirection?,
    onDragDirectionChanged: (SwipeDirection?) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onDirection: (SwipeDirection) -> Unit,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Crossfade(
            targetState = current,
            animationSpec = tween(durationMillis = if (animations) 260 else 0),
            label = "photo-card",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { item ->
            PhotoCardStack(
                current = item,
                next = state.next,
                bindings = state.gestureBindings,
                onOpenDetail = { onOpenPhoto(item.photo.id) },
                onSwipe = onDirection,
                onDragDirectionChanged = onDragDirectionChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = PhotidySpacing.lg,
                        end = PhotidySpacing.lg,
                        top = PhotidySpacing.sm,
                        bottom = PhotidySpacing.sm,
                    ),
            )
        }

        Text(
            text = stringResource(LocR.string.organizer_swipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = PhotidySpacing.lg,
                vertical = PhotidySpacing.xs,
            ),
        )

        OrganizerActionBar(
            bindings = state.gestureBindings,
            activeDirection = activeDirection,
            onAction = onAction,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(
                    start = PhotidySpacing.lg,
                    end = PhotidySpacing.lg,
                    bottom = PhotidySpacing.md,
                ),
        )
    }
}

@Composable
private fun OrganizerBatchArea(
    state: OrganizerUiState,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onApply: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val allSelected = state.batchItems.isNotEmpty() && state.batchIds.size == state.batchItems.size

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.batchItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PhotidyEmptyState(
                    title = stringResource(LocR.string.organizer_batch_empty),
                    message = stringResource(LocR.string.organizer_batch_empty_message),
                )
            }
        } else {
            Text(
                text = stringResource(LocR.string.organizer_batch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(vertical = PhotidySpacing.xs),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .padding(horizontal = PhotidySpacing.lg),
                contentPadding = PaddingValues(vertical = PhotidySpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            ) {
                items(state.batchItems, key = { it.photo.id }) { item ->
                    BatchTile(
                        item = item,
                        selected = item.photo.id in state.batchIds,
                        onClick = { onToggleSelection(item.photo.id) },
                    )
                }
            }
        }

        Text(
            text = stringResource(LocR.string.review_selected, state.batchIds.size),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(vertical = PhotidySpacing.xs),
        )

        Row(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = PhotidySpacing.lg, vertical = PhotidySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotidyTextButton(
                text = stringResource(
                    if (allSelected) {
                        LocR.string.review_deselect_all
                    } else {
                        LocR.string.review_select_all
                    },
                ),
                onClick = if (allSelected) onClearSelection else onSelectAll,
                enabled = state.batchItems.isNotEmpty(),
            )
            Spacer(Modifier.weight(1f))
            PhotidyTextButton(
                text = stringResource(LocR.string.organizer_batch_delete, state.batchIds.size),
                onClick = { onApply(GestureAction.ADD_TO_DELETE_LIST) },
                enabled = state.batchIds.isNotEmpty(),
                tint = colors.danger,
            )
            Spacer(Modifier.width(PhotidySpacing.xs))
            PhotidyTextButton(
                text = stringResource(LocR.string.organizer_batch_keep, state.batchIds.size),
                onClick = { onApply(GestureAction.KEEP) },
                enabled = state.batchIds.isNotEmpty(),
                tint = colors.accent,
            )
        }
    }
}

@Composable
private fun BatchTile(
    item: PhotoWithState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    val corner = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(corner)
            .background(colors.surface)
            .clickable(onClick = onClick),
    ) {
        PhotoImage(photo = item.photo, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.scrim.copy(alpha = 0.24f)),
                    ),
                ),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.accent.copy(alpha = 0.18f)),
            )
        }
        Box(
            modifier = Modifier
                .padding(PhotidySpacing.sm)
                .align(Alignment.TopEnd)
                .size(23.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else colors.scrim.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(LocR.string.cd_selected),
                    tint = colors.onAccent,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                )
            }
        }
    }
}

@Composable
private fun OrganizerActionBar(
    bindings: GestureBindings,
    activeDirection: SwipeDirection?,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val directions = listOf(
        SwipeDirection.LEFT,
        SwipeDirection.DOWN,
        SwipeDirection.UP,
        SwipeDirection.RIGHT,
    )
    PhotidySurface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(horizontal = PhotidySpacing.xs, vertical = PhotidySpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            directions.forEach { direction ->
                val action = bindings.actionFor(direction)
                val enabled = action != GestureAction.NONE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (activeDirection == direction) {
                                colors.accentSoft.copy(alpha = 0.55f)
                            } else {
                                Color.Transparent
                            },
                            RoundedCornerShape(18.dp),
                        )
                        .clickable(enabled = enabled) { onAction(action) }
                        .padding(vertical = PhotidySpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) {
                                    colors.accentSoft.copy(alpha = 0.7f)
                                } else {
                                    colors.backgroundSecondary
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = actionIcon(action),
                            contentDescription = stringResource(actionLabelRes(action)),
                            tint = if (enabled) {
                                actionColor(action, colors)
                            } else {
                                colors.textTertiary
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = stringResource(actionLabelRes(action)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) colors.textSecondary else colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextPanel(
    state: OrganizerUiState,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    Column(
        modifier = modifier.padding(vertical = PhotidySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
    ) {
        PhotidySurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(PhotidySpacing.lg),
        ) {
            PhotidySectionHeader(title = stringResource(LocR.string.organizer_up_next))
            if (state.upNext.isEmpty()) {
                Text(
                    text = stringResource(LocR.string.organizer_session_remaining, state.remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            } else {
                state.upNext.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        ) {
                            app.photidy.core.designsystem.artwork.PhotoImage(
                                photo = item.photo,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.photo.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = PhotidyFormatters.formatDate(item.photo.capturedAt, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        val stats = state.stats
        if (stats != null) {
            PhotidySurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(PhotidySpacing.lg),
            ) {
                PhotidySectionHeader(title = stringResource(LocR.string.organizer_session_stats))
                StatLine(
                    label = stringResource(
                        LocR.string.organizer_session_acted,
                        stats.reviewed,
                    ),
                )
                StatLine(
                    label = stringResource(
                        LocR.string.organizer_complete_delete,
                        stats.markedForDeletion,
                    ),
                )
                StatLine(
                    label = stringResource(
                        LocR.string.organizer_complete_favorites,
                        stats.favorites,
                    ),
                )
                StatLine(
                    label = stringResource(
                        LocR.string.organizer_session_remaining,
                        state.remaining,
                    ),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, emphasized: Boolean = false) {
    val colors = Theme.colors
    Text(
        text = label,
        style = if (emphasized) {
            MaterialTheme.typography.titleSmall
        } else {
            MaterialTheme.typography.bodySmall
        },
        color = if (emphasized) colors.textPrimary else colors.textSecondary,
    )
}

@Composable
private fun OrganizerCompletion(
    state: OrganizerUiState,
    onOpenReview: () -> Unit,
    onBackHome: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val stats = state.stats
    val kept = ((stats?.reviewed ?: 0) - (stats?.markedForDeletion ?: 0)).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .padding(PhotidySpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = stringResource(LocR.string.organizer_complete_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pluralStringResource(
                    LocR.plurals.reviewed_photos_message,
                    state.total,
                    state.total,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(PhotidySpacing.xs))
            if (stats != null) {
                PhotidySurface(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(PhotidySpacing.lg),
                ) {
                    StatLine(
                        label = stringResource(LocR.string.organizer_complete_kept, kept),
                    )
                    StatLine(
                        label = stringResource(
                            LocR.string.organizer_complete_delete,
                            stats.markedForDeletion,
                        ),
                    )
                    StatLine(
                        label = stringResource(
                            LocR.string.organizer_complete_favorites,
                            stats.favorites,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(PhotidySpacing.xs))
            PhotidyPrimaryButton(
                text = stringResource(LocR.string.organizer_complete_review),
                onClick = onOpenReview,
                modifier = Modifier.fillMaxWidth(),
            )
            PhotidySecondaryButton(
                text = stringResource(LocR.string.organizer_complete_home),
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.canUndo) {
                PhotidyTextButton(
                    text = stringResource(LocR.string.organizer_complete_undo),
                    onClick = onUndo,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    albums: List<Album>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Theme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = PhotidySpacing.md)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.glassBorder),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PhotidySpacing.xxl,
                    end = PhotidySpacing.xxl,
                    bottom = PhotidySpacing.xxxl,
                ),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
        ) {
            Text(
                text = stringResource(LocR.string.organizer_album_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(LocR.string.organizer_album_picker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(PhotidySpacing.sm))
            albums.forEach { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onChoose(album.id) }
                        .padding(vertical = PhotidySpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accentSoft.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = album.title ?: stringResource(AlbumNames.nameRes(album.id)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrganizerScreenPreview() {
    val photo = PhotoWithState(
        photo = app.photidy.core.model.PhotoItem(
            id = "preview",
            name = "IMG_1001",
            albumId = "camera",
            capturedAt = System.currentTimeMillis(),
            width = 4032,
            height = 3024,
            sizeBytes = 4_200_000,
            scene = PhotoScene.MOUNTAIN_DAWN,
            artSeed = 12,
        ),
        state = PhotoState(),
    )
    Theme(settings = app.photidy.core.model.AppSettings()) {
        OrganizerScreen(
            state = OrganizerUiState(
                isLoading = false,
                hasSession = true,
                current = photo,
                total = 20,
                cursor = 3,
                remaining = 17,
                canUndo = true,
                stats = SessionStatsUi(reviewed = 3, total = 20, markedForDeletion = 1, favorites = 1),
            ),
            onBack = {},
            onOpenReview = {},
            onOpenPhoto = {},
            onChoosePhotos = {},
            onAction = {},
            onDirection = {},
            onUndo = {},
            onChooseAlbum = {},
            onDismissAlbumPicker = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
