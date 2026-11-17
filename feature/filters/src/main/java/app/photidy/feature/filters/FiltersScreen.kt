package app.photidy.feature.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.data.AiModelState
import app.photidy.core.designsystem.components.PhotidyChip
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyProgressLine
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySectionHeader
import app.photidy.core.designsystem.components.PhotidySegment
import app.photidy.core.designsystem.components.PhotidySegmentedControl
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.AlbumNames
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.AiTag
import app.photidy.core.model.Album
import app.photidy.core.model.DateRangeFilter
import app.photidy.core.model.MediaFilter
import app.photidy.core.model.PhotoFilter
import app.photidy.core.model.PhotoSort
import app.photidy.core.model.SmartFilter
import kotlinx.coroutines.launch

@Composable
fun FiltersRoute(
    onBack: () -> Unit,
    onSessionStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FiltersViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    FiltersScreen(
        state = state,
        onBack = onBack,
        onStart = {
            scope.launch {
                if (viewModel.startSession() > 0) {
                    onSessionStarted()
                }
            }
        },
        onReset = viewModel::reset,
        onAlbumSelected = viewModel::selectAlbum,
        onMediaSelected = viewModel::selectMedia,
        onDateRangeSelected = viewModel::selectDateRange,
        onSortSelected = viewModel::selectSort,
        onSmartSelected = viewModel::selectSmart,
        onAiSelected = viewModel::selectAiTag,
        onUnorganizedOnlyChanged = viewModel::setUnorganizedOnly,
        onRecentSelected = viewModel::applyRecent,
        modifier = modifier,
    )
}

@Composable
fun FiltersScreen(
    state: FiltersUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onAlbumSelected: (String?) -> Unit,
    onMediaSelected: (MediaFilter) -> Unit,
    onDateRangeSelected: (DateRangeFilter) -> Unit,
    onSortSelected: (PhotoSort) -> Unit,
    onSmartSelected: (SmartFilter) -> Unit,
    onAiSelected: (AiTag?) -> Unit,
    onUnorganizedOnlyChanged: (Boolean) -> Unit,
    onRecentSelected: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass != PhotidyWidthClass.COMPACT
    val colors = Theme.colors

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.filters_title),
                subtitle = pluralStringResource(
                    LocR.plurals.photo_count,
                    state.matchCount,
                    state.matchCount,
                ),
                onBack = onBack,
                actions = {
                    PhotidyTextButton(
                        text = stringResource(LocR.string.filters_reset),
                        onClick = onReset,
                        tint = colors.accent,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                        vertical = PhotidySpacing.md,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (expanded) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 900.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            RecentSection(state, onRecentSelected)
                            AlbumSection(state, onAlbumSelected)
                            TimeSection(state.filter, onDateRangeSelected)
                            SmartSection(state.filter, onSmartSelected)
                            AiSection(state, onAiSelected)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            MediaSection(state.filter, onMediaSelected)
                            StatusSection(state.filter, onUnorganizedOnlyChanged)
                            SortSection(state.filter, onSortSelected)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                    ) {
                        RecentSection(state, onRecentSelected)
                        AlbumSection(state, onAlbumSelected)
                        MediaSection(state.filter, onMediaSelected)
                        TimeSection(state.filter, onDateRangeSelected)
                        SmartSection(state.filter, onSmartSelected)
                        AiSection(state, onAiSelected)
                        SortSection(state.filter, onSortSelected)
                        StatusSection(state.filter, onUnorganizedOnlyChanged)
                    }
                }
                Spacer(Modifier.height(PhotidySpacing.lg))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                        vertical = PhotidySpacing.md,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PhotidyPrimaryButton(
                    text = if (state.matchCount == 0) {
                        stringResource(LocR.string.filters_start_empty)
                    } else {
                        pluralStringResource(
                            LocR.plurals.start_with_photos,
                            state.matchCount,
                            state.matchCount,
                        )
                    },
                    onClick = onStart,
                    enabled = state.matchCount > 0 && !state.isLoading,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSection(
    state: FiltersUiState,
    onRecentSelected: (PhotoFilter) -> Unit,
) {
    if (state.recentFilters.isEmpty()) return
    FilterPanel(title = stringResource(LocR.string.filters_recent)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            state.recentFilters.forEach { recent ->
                PhotidyChip(
                    label = recentLabel(recent, state.albums),
                    selected = recent == state.filter,
                    onClick = { onRecentSelected(recent) },
                )
            }
        }
    }
}

/** Short, localised summary so a saved filter is recognisable at a glance. */
@Composable
private fun recentLabel(filter: PhotoFilter, albums: List<Album>): String {
    val parts = mutableListOf<String>()
    filter.albumId?.let { albumId ->
        parts += albums.firstOrNull { it.id == albumId }?.title
            ?: stringResource(AlbumNames.nameRes(albumId))
    }
    if (filter.smart != SmartFilter.NONE) {
        parts += stringResource(smartLabelRes(filter.smart))
    }
    filter.aiTag?.let { parts += stringResource(aiTagLabelRes(it)) }
    when (filter.media) {
        MediaFilter.PHOTOS -> parts += stringResource(LocR.string.filters_media_photos)
        MediaFilter.VIDEOS -> parts += stringResource(LocR.string.filters_media_videos)
        MediaFilter.ALL -> Unit
    }
    if (filter.dateRange != DateRangeFilter.ALL_TIME) {
        parts += stringResource(dateRangeLabelRes(filter.dateRange))
    }
    if (filter.unorganizedOnly) {
        parts += stringResource(LocR.string.filters_status_unreviewed)
    }
    return if (parts.isEmpty()) {
        stringResource(LocR.string.filters_recent_all)
    } else {
        parts.take(2).joinToString(" · ")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlbumSection(
    state: FiltersUiState,
    onAlbumSelected: (String?) -> Unit,
) {
    FilterPanel(title = stringResource(LocR.string.filters_album)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            PhotidyChip(
                label = stringResource(LocR.string.filters_album_all),
                selected = state.filter.albumId == null,
                onClick = { onAlbumSelected(null) },
            )
            state.albums.forEach { album: Album ->
                PhotidyChip(
                    label = album.title ?: stringResource(AlbumNames.nameRes(album.id)),
                    selected = state.filter.albumId == album.id,
                    onClick = { onAlbumSelected(album.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartSection(
    filter: PhotoFilter,
    onSmartSelected: (SmartFilter) -> Unit,
) {
    val albumSelected = filter.albumId != null
    FilterPanel(title = stringResource(LocR.string.filters_smart)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            SmartFilter.entries.forEach { smart ->
                PhotidyChip(
                    label = stringResource(smartLabelRes(smart)),
                    selected = filter.smart == smart,
                    onClick = { onSmartSelected(smart) },
                    enabled = albumSelected,
                )
            }
        }
        if (!albumSelected) {
            Spacer(Modifier.height(PhotidySpacing.xs))
            Text(
                text = stringResource(LocR.string.filters_smart_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.textTertiary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiSection(
    state: FiltersUiState,
    onAiSelected: (AiTag?) -> Unit,
) {
    val ready = state.aiEnabled &&
        state.aiModel is AiModelState.Ready &&
        state.hasDevicePhotos

    FilterPanel(title = stringResource(LocR.string.filters_ai_title)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            PhotidyChip(
                label = stringResource(LocR.string.filters_ai_none),
                selected = state.filter.aiTag == null,
                onClick = { onAiSelected(null) },
                enabled = ready,
            )
            AiTag.entries.forEach { tag ->
                PhotidyChip(
                    label = stringResource(aiTagLabelRes(tag)),
                    selected = state.filter.aiTag == tag,
                    onClick = { onAiSelected(tag) },
                    enabled = ready,
                )
            }
        }
        Spacer(Modifier.height(PhotidySpacing.xs))
        when {
            !state.aiEnabled -> AiHint(LocR.string.filters_ai_disabled_hint)
            state.aiModel is AiModelState.Failed -> AiHint(LocR.string.filters_ai_failed_hint)
            state.aiModel !is AiModelState.Ready -> AiHint(LocR.string.filters_ai_missing_hint)
            !state.hasDevicePhotos -> AiHint(LocR.string.filters_ai_demo_hint)
            state.aiProgress.analyzing && state.aiProgress.total > 0 -> {
                Text(
                    text = stringResource(
                        LocR.string.filters_ai_progress,
                        state.aiProgress.done,
                        state.aiProgress.total,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.textSecondary,
                )
                Spacer(Modifier.height(PhotidySpacing.xxs))
                PhotidyProgressLine(
                    progress = state.aiProgress.done.toFloat() / state.aiProgress.total,
                )
            }
            else -> AiHint(LocR.string.filters_ai_hint)
        }
    }
}

@Composable
private fun AiHint(res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodySmall,
        color = Theme.colors.textTertiary,
    )
}

private fun smartLabelRes(smart: SmartFilter): Int = when (smart) {
    SmartFilter.NONE -> LocR.string.filters_smart_none
    SmartFilter.SCREENSHOTS -> LocR.string.filters_smart_screenshots
    SmartFilter.POSSIBLE_DUPLICATES -> LocR.string.filters_smart_duplicates
    SmartFilter.BURST_SHOTS -> LocR.string.filters_smart_burst
    SmartFilter.LONG_VIDEOS -> LocR.string.filters_smart_long_videos
    SmartFilter.LARGE_FILES -> LocR.string.filters_smart_large_files
}

private fun dateRangeLabelRes(range: DateRangeFilter): Int = when (range) {
    DateRangeFilter.ALL_TIME -> LocR.string.filters_date_all
    DateRangeFilter.LAST_30_DAYS -> LocR.string.filters_date_30
    DateRangeFilter.LAST_90_DAYS -> LocR.string.filters_date_90
    DateRangeFilter.THIS_YEAR -> LocR.string.filters_date_year
}

private fun aiTagLabelRes(tag: AiTag): Int = when (tag) {
    AiTag.FOOD -> LocR.string.filters_ai_food
    AiTag.PET -> LocR.string.filters_ai_pet
    AiTag.DOCUMENT -> LocR.string.filters_ai_document
    AiTag.NATURE -> LocR.string.filters_ai_nature
    AiTag.LANDMARK -> LocR.string.filters_ai_landmark
}

@Composable
private fun MediaSection(
    filter: PhotoFilter,
    onMediaSelected: (MediaFilter) -> Unit,
) {
    FilterPanel(title = stringResource(LocR.string.filters_media)) {
        PhotidySegmentedControl(
            options = listOf(
                PhotidySegment(MediaFilter.ALL, stringResource(LocR.string.filters_media_all)),
                PhotidySegment(MediaFilter.PHOTOS, stringResource(LocR.string.filters_media_photos)),
                PhotidySegment(MediaFilter.VIDEOS, stringResource(LocR.string.filters_media_videos)),
            ),
            selected = filter.media,
            onSelect = onMediaSelected,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSection(
    filter: PhotoFilter,
    onDateRangeSelected: (DateRangeFilter) -> Unit,
) {
    FilterPanel(title = stringResource(LocR.string.filters_date)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            DateRangeFilter.entries.forEach { range ->
                PhotidyChip(
                    label = stringResource(dateRangeLabelRes(range)),
                    selected = filter.dateRange == range,
                    onClick = { onDateRangeSelected(range) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortSection(
    filter: PhotoFilter,
    onSortSelected: (PhotoSort) -> Unit,
) {
    FilterPanel(title = stringResource(LocR.string.filters_sort)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            PhotoSort.entries.forEach { sort ->
                val label = when (sort) {
                    PhotoSort.NEWEST_FIRST -> stringResource(LocR.string.filters_sort_newest)
                    PhotoSort.OLDEST_FIRST -> stringResource(LocR.string.filters_sort_oldest)
                    PhotoSort.LARGEST_FIRST -> stringResource(LocR.string.filters_sort_largest)
                    PhotoSort.RANDOM -> stringResource(LocR.string.filters_sort_random)
                }
                PhotidyChip(
                    label = label,
                    selected = filter.sort == sort,
                    onClick = { onSortSelected(sort) },
                )
            }
        }
    }
}

@Composable
private fun StatusSection(
    filter: PhotoFilter,
    onUnorganizedOnlyChanged: (Boolean) -> Unit,
) {
    FilterPanel(title = stringResource(LocR.string.filters_status)) {
        PhotidySegmentedControl(
            options = listOf(
                PhotidySegment(false, stringResource(LocR.string.filters_status_all)),
                PhotidySegment(true, stringResource(LocR.string.filters_status_unreviewed)),
            ),
            selected = filter.unorganizedOnly,
            onSelect = onUnorganizedOnlyChanged,
        )
    }
}

@Composable
private fun FilterPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = title)
        Spacer(Modifier.height(PhotidySpacing.sm))
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun FiltersScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        FiltersScreen(
            state = FiltersUiState(
                isLoading = false,
                matchCount = 32,
                albums = listOf(Album("camera"), Album("travel")),
            ),
            onBack = {},
            onStart = {},
            onReset = {},
            onAlbumSelected = {},
            onMediaSelected = {},
            onDateRangeSelected = {},
            onSortSelected = {},
            onSmartSelected = {},
            onAiSelected = {},
            onUnorganizedOnlyChanged = {},
            onRecentSelected = {},
        )
    }
}
