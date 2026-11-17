package app.photidy.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyBadge
import app.photidy.core.designsystem.components.PhotidyBadgeTone
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoScene
import java.util.Locale

@Composable
fun DuplicatesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DuplicatesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DuplicatesScreen(
        state = state,
        onBack = onBack,
        onChooseKeeper = viewModel::chooseKeeper,
        onSkip = viewModel::skip,
        onApply = viewModel::applyGroup,
        onUndo = viewModel::undoLast,
        onRestoreSkipped = viewModel::restoreSkipped,
        modifier = modifier,
    )
}

@Composable
fun DuplicatesScreen(
    state: DuplicatesUiState,
    onBack: () -> Unit,
    onChooseKeeper: (String, String) -> Unit,
    onSkip: (String) -> Unit,
    onApply: (String) -> Unit,
    onUndo: () -> Unit,
    onRestoreSkipped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale: Locale = LocalConfiguration.current.locales[0]

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.duplicates_title),
                subtitle = if (state.totalGroups == 0) null else {
                    stringResource(
                        LocR.string.duplicates_subtitle,
                        pluralStringResource(
                            LocR.plurals.duplicate_groups,
                            state.totalGroups,
                            state.totalGroups,
                        ),
                        PhotidyFormatters.formatBytes(state.reclaimableBytes, locale),
                    )
                },
                onBack = onBack,
                actions = {
                    if (state.skippedCount > 0) {
                        PhotidyTextButton(
                            text = stringResource(
                                LocR.string.duplicates_restore,
                                state.skippedCount,
                            ),
                            onClick = onRestoreSkipped,
                        )
                    }
                },
            )

            state.lastApplied?.let { applied ->
                UndoStrip(
                    count = applied.flaggedIds.size,
                    onUndo = onUndo,
                )
            }

            if (state.isLoading) {
                Spacer(Modifier.weight(1f))
            } else if (state.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.duplicates_empty_title),
                        message = stringResource(LocR.string.duplicates_empty_message),
                        icon = Icons.AutoMirrored.Filled.List,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = PhotidySpacing.lg,
                        vertical = PhotidySpacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                ) {
                    items(state.groups, key = { it.key }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            locale = locale,
                            onChooseKeeper = { photoId -> onChooseKeeper(group.key, photoId) },
                            onSkip = { onSkip(group.key) },
                            onApply = { onApply(group.key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UndoStrip(
    count: Int,
    onUndo: () -> Unit,
) {
    PhotidySurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PhotidySpacing.lg, vertical = PhotidySpacing.xs),
        contentPadding = PaddingValues(horizontal = PhotidySpacing.lg, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(
                    LocR.plurals.duplicates_applied,
                    count,
                    count,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            PhotidyTextButton(
                text = stringResource(LocR.string.common_undo),
                onClick = onUndo,
                tint = Theme.colors.accent,
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroupUi,
    locale: Locale,
    onChooseKeeper: (String) -> Unit,
    onSkip: () -> Unit,
    onApply: () -> Unit,
) {
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.md),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm)) {
            items(group.photos, key = { it.id }) { photo ->
                DuplicateTile(
                    photo = photo,
                    isKeeper = photo.id == group.keepId,
                    onClick = { onChooseKeeper(photo.id) },
                )
            }
        }
        Text(
            text = pluralStringResource(
                LocR.plurals.duplicate_others,
                group.others.size,
                group.others.size,
                PhotidyFormatters.formatBytes(group.reclaimableBytes, locale),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Theme.colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotidyTextButton(
                text = stringResource(LocR.string.duplicates_skip),
                onClick = onSkip,
            )
            Spacer(Modifier.weight(1f))
            PhotidyPrimaryButton(
                text = stringResource(LocR.string.duplicates_apply),
                onClick = onApply,
            )
        }
    }
}

@Composable
private fun DuplicateTile(
    photo: PhotoItem,
    isKeeper: Boolean,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .width(122.dp)
            .height(138.dp)
            .clip(shape)
            .background(colors.surface)
            .border(
                width = if (isKeeper) 2.5.dp else 1.dp,
                color = if (isKeeper) colors.accent else colors.glassBorder,
                shape = shape,
            )
            .selectable(
                selected = isKeeper,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        PhotoImage(
            photo = photo,
            modifier = Modifier.fillMaxSize(),
            thumbnail = true,
        )
        // Dim the photos that would be flagged, so the keeper stands out at a glance.
        if (!isKeeper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, colors.scrim.copy(alpha = 0.42f)),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .padding(PhotidySpacing.xs)
                .align(Alignment.TopStart),
        ) {
            PhotidyBadge(
                text = stringResource(
                    if (isKeeper) LocR.string.duplicates_keep else LocR.string.duplicates_flag,
                ),
                tone = if (isKeeper) PhotidyBadgeTone.ACCENT else PhotidyBadgeTone.DANGER,
            )
        }
        Text(
            text = PhotidyFormatters.formatDimensions(photo.width, photo.height),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PhotidySpacing.xs),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DuplicatesScreenPreview() {
    val photo = PhotoItem(
        id = "a",
        name = "IMG_1001.jpg",
        albumId = "camera",
        capturedAt = 1_760_000_000_000,
        width = 4032,
        height = 3024,
        sizeBytes = 4_200_000,
        scene = PhotoScene.LAKE_REFLECTION,
        artSeed = 3,
    )
    Theme(settings = app.photidy.core.model.AppSettings()) {
        DuplicatesScreen(
            state = DuplicatesUiState(
                isLoading = false,
                groups = listOf(
                    DuplicateGroupUi(
                        key = "a+b+c",
                        photos = listOf(
                            photo,
                            photo.copy(id = "b", scene = PhotoScene.MOUNTAIN_DAWN),
                            photo.copy(id = "c", scene = PhotoScene.CITY_NIGHT),
                        ),
                        keepId = "a",
                    ),
                ),
                totalGroups = 1,
                reclaimableBytes = 8_400_000,
            ),
            onBack = {},
            onChooseKeeper = { _, _ -> },
            onSkip = {},
            onApply = {},
            onUndo = {},
            onRestoreSkipped = {},
        )
    }
}
