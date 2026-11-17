package app.photidy.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyBadge
import app.photidy.core.designsystem.components.PhotidyChip
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoScene
import app.photidy.core.model.TimelineSection
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun TimelineRoute(
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TimelineViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TimelineScreen(
        state = state,
        onBack = onBack,
        onOpenPhoto = onOpenPhoto,
        modifier = modifier,
    )
}

@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Item offsets let the month chips jump straight to a section inside the grid.
    val sectionOffsets = remember(state.sections) {
        var index = 0
        state.sections.map { section ->
            val start = index
            index += 1 + section.photos.size
            start
        }
    }
    val activeSection by remember(sectionOffsets) {
        derivedStateOf {
            sectionOffsets
                .indexOfLast { it <= gridState.firstVisibleItemIndex }
                .coerceAtLeast(0)
        }
    }
    val monthLabels = state.sections.map { section ->
        if (section.isDated) {
            stringResource(
                LocR.string.timeline_month_header,
                PhotidyFormatters.formatMonthShort(section.year, section.month, locale),
                section.year,
            )
        } else {
            stringResource(LocR.string.timeline_unknown_date)
        }
    }

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.timeline_title),
                subtitle = if (state.totalPhotos == 0) null else {
                    stringResource(
                        LocR.string.timeline_subtitle,
                        pluralStringResource(
                            LocR.plurals.photo_count,
                            state.totalPhotos,
                            state.totalPhotos,
                        ),
                        pluralStringResource(
                            LocR.plurals.timeline_months,
                            state.sections.size,
                            state.sections.size,
                        ),
                    )
                },
                onBack = onBack,
            )

            if (state.sections.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.timeline_empty_title),
                        message = stringResource(LocR.string.timeline_empty_message),
                        icon = Icons.Filled.PlayArrow,
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = PhotidySpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                ) {
                    state.sections.forEachIndexed { index, _ ->
                        item(key = "chip-$index") {
                            PhotidyChip(
                                label = monthLabels[index],
                                selected = index == activeSection,
                                onClick = {
                                    scope.launch {
                                        gridState.animateScrollToItem(sectionOffsets[index])
                                    }
                                },
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    state = gridState,
                    modifier = Modifier
                        .widthIn(max = 1100.dp)
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = PhotidySpacing.sm),
                    contentPadding = PaddingValues(
                        horizontal = PhotidySpacing.lg,
                        vertical = PhotidySpacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                ) {
                    state.sections.forEachIndexed { index, section ->
                        item(
                            key = "header-${section.year}-${section.month}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            TimelineHeader(
                                label = monthLabels[index],
                                count = section.photos.size,
                            )
                        }
                        items(section.photos, key = { it.id }) { photo ->
                            TimelineTile(
                                photo = photo,
                                onClick = { onOpenPhoto(photo.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PhotidySpacing.sm, bottom = PhotidySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Theme.colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        PhotidyBadge(
            text = pluralStringResource(LocR.plurals.photo_count, count, count),
        )
    }
}

@Composable
private fun TimelineTile(
    photo: PhotoItem,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surface)
            .clickable(onClick = onClick),
    ) {
        PhotoImage(
            photo = photo,
            modifier = Modifier.fillMaxSize(),
        )
        val duration = photo.durationSeconds
        if (photo.isVideo && duration != null) {
            Row(
                modifier = Modifier
                    .padding(PhotidySpacing.xs)
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
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = PhotidyFormatters.formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineScreenPreview() {
    val photo = PhotoItem(
        id = "a",
        name = "IMG_1001.jpg",
        albumId = "camera",
        capturedAt = 1_760_000_000_000,
        width = 4032,
        height = 3024,
        sizeBytes = 4_200_000,
        scene = PhotoScene.MOUNTAIN_DAWN,
        artSeed = 2,
    )
    Theme(settings = app.photidy.core.model.AppSettings()) {
        TimelineScreen(
            state = TimelineUiState(
                isLoading = false,
                sections = listOf(
                    TimelineSection(2026, 11, listOf(photo, photo.copy(id = "b"))),
                    TimelineSection(2026, 10, listOf(photo.copy(id = "c"))),
                ),
                totalPhotos = 3,
            ),
            onBack = {},
            onOpenPhoto = {},
        )
    }
}
