package app.photidy.feature.review

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyConfirmDialog
import app.photidy.core.designsystem.components.PhotidyDivider
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.TrashWindow
import java.util.Locale

@Composable
fun ReviewRoute(
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onStartTidying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReviewViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val unsupportedMessage = stringResource(LocR.string.review_delete_unsupported)
    val failedMessage = stringResource(LocR.string.review_delete_failed)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewEvent.RequestDelete -> launcher.launch(
                    IntentSenderRequest.Builder(event.intentSender).build(),
                )
                ReviewEvent.DeleteUnsupported -> snackbarHostState.showSnackbar(unsupportedMessage)
                ReviewEvent.DeleteFailed -> snackbarHostState.showSnackbar(failedMessage)
                is ReviewEvent.Deleted -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(
                        LocR.plurals.review_delete_done,
                        event.count,
                        event.count,
                    ),
                )
            }
        }
    }

    ReviewScreen(
        state = state,
        onBack = onBack,
        onOpenPhoto = onOpenPhoto,
        onStartTidying = onStartTidying,
        onToggleSelectionMode = viewModel::toggleSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onEnterSelection = viewModel::enterSelection,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onRemoveSelected = viewModel::removeSelected,
        onDeleteSelected = viewModel::deleteSelected,
        onClearAll = viewModel::clearAll,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onStartTidying: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onEnterSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRemoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass != PhotidyWidthClass.COMPACT
    val locale: Locale = LocalConfiguration.current.locales[0]
    val allSelected = state.items.isNotEmpty() && state.selectedIds.size == state.items.size
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    PhotidyScreen(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                PhotidyTopBar(
                    title = stringResource(LocR.string.review_title),
                    onBack = onBack,
                    actions = {
                        if (state.items.isNotEmpty()) {
                            PhotidyTextButton(
                                text = if (state.selectionMode) {
                                    stringResource(LocR.string.review_cancel_selection)
                                } else {
                                    stringResource(LocR.string.review_select)
                                },
                                onClick = onToggleSelectionMode,
                                tint = Theme.colors.accent,
                            )
                        }
                    },
                )

                if (state.trashCount > 0) {
                    TrashNotice(
                        count = state.trashCount,
                        daysLeft = TrashWindow.daysLeft(
                            expiresAtMillis = state.trashExpiryMillis,
                            now = System.currentTimeMillis(),
                        ),
                        modifier = Modifier.padding(
                            start = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                            end = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                            top = PhotidySpacing.xs,
                        ),
                    )
                }

                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PhotidyEmptyState(
                            title = stringResource(LocR.string.review_empty_title),
                            message = stringResource(LocR.string.review_empty_message),
                            icon = Icons.Filled.Delete,
                            actionLabel = stringResource(LocR.string.review_empty_action),
                            onAction = onStartTidying,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(
                                horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                                vertical = PhotidySpacing.sm,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PhotidySurface(
                            modifier = Modifier
                                .widthIn(max = 1100.dp)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(PhotidySpacing.lg),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pluralStringResource(
                                            LocR.plurals.review_summary_plural,
                                            state.items.size,
                                            state.items.size,
                                            PhotidyFormatters.formatBytes(state.totalBytes, locale),
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Theme.colors.textPrimary,
                                    )
                                    Text(
                                        text = stringResource(LocR.string.review_safety_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Theme.colors.textTertiary,
                                    )
                                }
                                PhotidyTextButton(
                                    text = stringResource(LocR.string.review_clear_all),
                                    onClick = { showClearDialog = true },
                                    tint = Theme.colors.danger,
                                )
                            }
                        }

                        Spacer(Modifier.height(PhotidySpacing.md))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(if (expanded) 148.dp else 104.dp),
                            modifier = Modifier
                                .widthIn(max = 1100.dp)
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 0.dp,
                                end = 0.dp,
                                top = 0.dp,
                                bottom = PhotidySpacing.lg,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                        ) {
                            items(state.items, key = { it.photo.id }) { item ->
                                ReviewTile(
                                    item = item,
                                    selected = item.photo.id in state.selectedIds,
                                    selectionMode = state.selectionMode,
                                    onClick = {
                                        if (state.selectionMode) {
                                            onToggleSelection(item.photo.id)
                                        } else {
                                            onOpenPhoto(item.photo.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!state.selectionMode) {
                                            onEnterSelection(item.photo.id)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (state.selectionMode) {
                        PhotidyDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PhotidySpacing.lg, vertical = PhotidySpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhotidyTextButton(
                                text = if (allSelected) {
                                    stringResource(LocR.string.review_deselect_all)
                                } else {
                                    stringResource(LocR.string.review_select_all)
                                },
                                onClick = if (allSelected) onClearSelection else onSelectAll,
                            )
                            Spacer(Modifier.weight(1f))
                            PhotidyTextButton(
                                text = stringResource(
                                    LocR.string.review_remove_selected,
                                    state.selectedIds.size,
                                ),
                                onClick = onRemoveSelected,
                                enabled = state.selectedIds.isNotEmpty(),
                            )
                            Spacer(Modifier.width(PhotidySpacing.xs))
                            PhotidyTextButton(
                                text = stringResource(
                                    LocR.string.review_delete_selected,
                                    state.selectedIds.size,
                                ),
                                onClick = { showDeleteDialog = true },
                                enabled = state.deletableCount > 0,
                                tint = Theme.colors.danger,
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PhotidySpacing.lg),
            )
        }
    }

    if (showClearDialog) {
        PhotidyConfirmDialog(
            title = stringResource(LocR.string.review_clear_title),
            message = pluralStringResource(
                LocR.plurals.clear_delete_message,
                state.items.size,
                state.items.size,
            ),
            confirmLabel = stringResource(LocR.string.common_clear),
            dismissLabel = stringResource(LocR.string.common_cancel),
            onConfirm = {
                showClearDialog = false
                onClearAll()
            },
            onDismiss = { showClearDialog = false },
            destructive = true,
        )
    }

    if (showDeleteDialog) {
        PhotidyConfirmDialog(
            title = stringResource(LocR.string.review_delete_confirm_title),
            message = stringResource(LocR.string.review_delete_confirm_message),
            confirmLabel = stringResource(LocR.string.review_delete_action),
            dismissLabel = stringResource(LocR.string.common_cancel),
            onConfirm = {
                showDeleteDialog = false
                onDeleteSelected()
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true,
        )
    }
}

@Composable
private fun TrashNotice(
    count: Int,
    daysLeft: Int,
    modifier: Modifier = Modifier,
) {
    PhotidySurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.md),
    ) {
        Text(
            text = if (daysLeft > 0) {
                pluralStringResource(
                    LocR.plurals.review_trash_notice,
                    count,
                    count,
                    daysLeft,
                )
            } else {
                pluralStringResource(
                    LocR.plurals.review_trash_notice_unknown,
                    count,
                    count,
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = Theme.colors.textSecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReviewTile(
    item: PhotoWithState,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = Theme.colors
    val corner = RoundedCornerShape(16.dp)
    val interaction = if (selectionMode) {
        Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
    } else {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(corner)
            .background(colors.surface)
            .then(interaction),
    ) {
        PhotoImage(
            photo = item.photo,
            modifier = Modifier.fillMaxSize(),
            thumbnail = true,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.scrim.copy(alpha = 0.28f)),
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

        if (item.state.favorite) {
            Box(
                modifier = Modifier
                    .padding(PhotidySpacing.sm)
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.scrim.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(LocR.string.cd_favorite),
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        if (selectionMode) {
            SelectionMark(
                selected = selected,
                modifier = Modifier
                    .padding(PhotidySpacing.sm)
                    .align(Alignment.TopEnd),
            )
        }

        val videoDuration = item.photo.durationSeconds
        if (item.photo.isVideo && videoDuration != null) {
            Row(
                modifier = Modifier
                    .padding(PhotidySpacing.sm)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(colors.scrim.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = PhotidyFormatters.formatDuration(videoDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = Theme.colors
    Box(
        modifier = modifier
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

@Preview(showBackground = true)
@Composable
private fun ReviewScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        ReviewScreen(
            state = ReviewUiState(
                isLoading = false,
                items = emptyList(),
            ),
            onBack = {},
            onOpenPhoto = {},
            onStartTidying = {},
            onToggleSelectionMode = {},
            onToggleSelection = {},
            onEnterSelection = {},
            onSelectAll = {},
            onClearSelection = {},
            onRemoveSelected = {},
            onDeleteSelected = {},
            onClearAll = {},
        )
    }
}
