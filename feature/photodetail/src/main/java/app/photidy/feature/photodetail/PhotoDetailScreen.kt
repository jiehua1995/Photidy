package app.photidy.feature.photodetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyBadge
import app.photidy.core.designsystem.components.PhotidyBadgeTone
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySecondaryButton
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.AlbumNames
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoDecision
import app.photidy.core.model.PhotoScene
import app.photidy.core.model.PhotoState
import app.photidy.core.model.PhotoWithState
import java.util.Locale

@Composable
fun PhotoDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PhotoDetailViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PhotoDetailScreen(
        state = state,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onMarkForDeletion = viewModel::markForDeletion,
        onMarkAsKept = viewModel::markAsKept,
        onRemoveFromDeleteList = viewModel::removeFromDeleteList,
        modifier = modifier,
    )
}

@Composable
fun PhotoDetailScreen(
    state: PhotoDetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkForDeletion: () -> Unit,
    onMarkAsKept: () -> Unit,
    onRemoveFromDeleteList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass == PhotidyWidthClass.EXPANDED
    val locale: Locale = LocalConfiguration.current.locales[0]
    val item = state.item

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.detail_title),
                onBack = onBack,
            )

            if (!state.isLoading && item == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.detail_not_found),
                        message = "",
                    )
                }
            } else if (item != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                            vertical = PhotidySpacing.sm,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (expanded) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 1040.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.xxl),
                            verticalAlignment = Alignment.Top,
                        ) {
                            DetailPhoto(
                                item = item,
                                maxHeight = 520.dp,
                                modifier = Modifier.weight(1.25f),
                            )
                            DetailInfo(
                                item = item,
                                locale = locale,
                                onToggleFavorite = onToggleFavorite,
                                onMarkForDeletion = onMarkForDeletion,
                                onMarkAsKept = onMarkAsKept,
                                onRemoveFromDeleteList = onRemoveFromDeleteList,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            DetailPhoto(item = item, maxHeight = 440.dp)
                            DetailInfo(
                                item = item,
                                locale = locale,
                                onToggleFavorite = onToggleFavorite,
                                onMarkForDeletion = onMarkForDeletion,
                                onMarkAsKept = onMarkAsKept,
                                onRemoveFromDeleteList = onRemoveFromDeleteList,
                            )
                        }
                    }
                    Spacer(Modifier.height(PhotidySpacing.xxxl))
                }
            }
        }
    }
}

@Composable
private fun DetailPhoto(
    item: PhotoWithState,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val photo = item.photo
    val videoDuration = photo.durationSeconds
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val ratio = photo.aspectRatio.coerceAtLeast(0.4f)
        val widthByHeight = maxHeight * ratio
        val width = minOf(maxWidth, widthByHeight)
        val height = width / ratio
        val corner = RoundedCornerShape(24.dp)

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(corner)
                .border(1.dp, colors.glassBorder, corner)
                .background(colors.surface),
        ) {
            PhotoImage(
                photo = photo,
                modifier = Modifier.fillMaxSize(),
                contentDescription = photo.name,
                contentScale = ContentScale.Fit,
            )
            if (photo.isVideo && videoDuration != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(PhotidySpacing.md)
                        .clip(CircleShape)
                        .background(colors.scrim.copy(alpha = 0.45f))
                        .padding(horizontal = PhotidySpacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailInfo(
    item: PhotoWithState,
    locale: Locale,
    onToggleFavorite: () -> Unit,
    onMarkForDeletion: () -> Unit,
    onMarkAsKept: () -> Unit,
    onRemoveFromDeleteList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val photo = item.photo
    val state = item.state
    val isDeleteCandidate = state.decision == PhotoDecision.DELETE_CANDIDATE
    val videoDuration = photo.durationSeconds

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xxs)) {
            Text(
                text = photo.name,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm)) {
                PhotidyBadge(
                    text = stringResource(statusLabelRes(state.decision)),
                    tone = when (state.decision) {
                        PhotoDecision.DELETE_CANDIDATE -> PhotidyBadgeTone.DANGER
                        PhotoDecision.KEPT -> PhotidyBadgeTone.ACCENT
                        PhotoDecision.UNREVIEWED -> PhotidyBadgeTone.NEUTRAL
                    },
                )
                if (state.favorite) {
                    PhotidyBadge(
                        text = stringResource(LocR.string.detail_favorite_on),
                        tone = PhotidyBadgeTone.ACCENT,
                    )
                }
            }
        }

        PhotidySurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(PhotidySpacing.lg),
        ) {
            MetaRow(
                label = stringResource(LocR.string.detail_date),
                value = PhotidyFormatters.formatDate(photo.capturedAt, locale),
            )
            MetaRow(
                label = stringResource(LocR.string.detail_resolution),
                value = PhotidyFormatters.formatDimensions(photo.width, photo.height),
            )
            MetaRow(
                label = stringResource(LocR.string.detail_size),
                value = PhotidyFormatters.formatBytes(photo.sizeBytes, locale),
            )
            MetaRow(
                label = stringResource(LocR.string.detail_album),
                value = photo.albumTitle
                    ?: stringResource(AlbumNames.nameRes(state.albumOverrideId ?: photo.albumId)),
            )
            if (photo.isVideo && videoDuration != null) {
                MetaRow(
                    label = stringResource(LocR.string.detail_duration),
                    value = PhotidyFormatters.formatDuration(videoDuration),
                )
            }
        }

        Text(
            text = stringResource(LocR.string.detail_safety_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            PhotidyPrimaryButton(
                text = stringResource(
                    if (state.favorite) {
                        LocR.string.detail_action_unfavorite
                    } else {
                        LocR.string.detail_action_favorite
                    },
                ),
                onClick = onToggleFavorite,
                leadingIcon = Icons.Filled.Favorite,
            )
            if (isDeleteCandidate) {
                PhotidySecondaryButton(
                    text = stringResource(LocR.string.detail_action_remove_delete),
                    onClick = onRemoveFromDeleteList,
                )
            } else {
                PhotidySecondaryButton(
                    text = stringResource(LocR.string.detail_action_delete),
                    onClick = onMarkForDeletion,
                )
            }
            if (state.decision != PhotoDecision.KEPT) {
                PhotidySecondaryButton(
                    text = stringResource(LocR.string.detail_action_keep),
                    onClick = onMarkAsKept,
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val colors = Theme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
        )
    }
}

private fun statusLabelRes(decision: PhotoDecision): Int = when (decision) {
    PhotoDecision.UNREVIEWED -> LocR.string.detail_status_unreviewed
    PhotoDecision.KEPT -> LocR.string.detail_status_kept
    PhotoDecision.DELETE_CANDIDATE -> LocR.string.detail_status_delete
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        PhotoDetailScreen(
            state = PhotoDetailUiState(
                isLoading = false,
                item = PhotoWithState(
                    photo = app.photidy.core.model.PhotoItem(
                        id = "preview",
                        name = "IMG_1001",
                        albumId = "travel",
                        capturedAt = System.currentTimeMillis(),
                        width = 4032,
                        height = 3024,
                        sizeBytes = 4_200_000,
                        scene = PhotoScene.OCEAN_DUSK,
                        artSeed = 7,
                        paletteIndex = 1,
                    ),
                    state = PhotoState(decision = PhotoDecision.DELETE_CANDIDATE, favorite = true),
                ),
            ),
            onBack = {},
            onToggleFavorite = {},
            onMarkForDeletion = {},
            onMarkAsKept = {},
            onRemoveFromDeleteList = {},
        )
    }
}
