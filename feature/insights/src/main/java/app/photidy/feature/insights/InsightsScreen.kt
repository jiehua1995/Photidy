package app.photidy.feature.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.components.PhotidyDivider
import app.photidy.core.designsystem.components.PhotidyMetricTile
import app.photidy.core.designsystem.components.PhotidyProgressLine
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySectionHeader
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.AlbumUsage
import app.photidy.core.model.InsightsSummary
import app.photidy.core.model.MonthCount
import java.time.YearMonth
import java.util.Locale

@Composable
fun InsightsRoute(
    onBack: () -> Unit,
    onOpenDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onBack = onBack,
        onOpenDuplicates = onOpenDuplicates,
        modifier = modifier,
    )
}

@Composable
fun InsightsScreen(
    state: InsightsSummary,
    onBack: () -> Unit,
    onOpenDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass == PhotidyWidthClass.EXPANDED
    val locale: Locale = LocalConfiguration.current.locales[0]

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.insights_title),
                onBack = onBack,
            )
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
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                ) {
                    MetricGrid(state = state, expanded = expanded, locale = locale)

                    CompositionCard(state = state, locale = locale)

                    MonthlyCard(state = state, locale = locale)

                    if (expanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                            StorageCard(
                                state = state,
                                locale = locale,
                                modifier = Modifier.weight(1f),
                            )
                            SessionCard(
                                state = state,
                                locale = locale,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        StorageCard(state = state, locale = locale)
                        SessionCard(state = state, locale = locale)
                    }

                    DuplicatesCard(
                        state = state,
                        locale = locale,
                        onOpenDuplicates = onOpenDuplicates,
                    )
                }
                Spacer(Modifier.height(PhotidySpacing.xxl))
            }
        }
    }
}

@Composable
private fun MetricGrid(
    state: InsightsSummary,
    expanded: Boolean,
    locale: Locale,
) {
    if (expanded) {
        Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
            MetricTile(state.totalPhotos, LocR.string.insights_total, locale, Modifier.weight(1f))
            MetricTile(
                state.organizedCount,
                LocR.string.insights_organized,
                locale,
                Modifier.weight(1f),
            )
            MetricTile(
                state.deleteCandidateCount,
                LocR.string.insights_delete,
                locale,
                Modifier.weight(1f),
            )
            MetricTile(
                state.favoriteCount,
                LocR.string.insights_favorites,
                locale,
                Modifier.weight(1f),
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                MetricTile(
                    state.totalPhotos,
                    LocR.string.insights_total,
                    locale,
                    Modifier.weight(1f),
                )
                MetricTile(
                    state.organizedCount,
                    LocR.string.insights_organized,
                    locale,
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                MetricTile(
                    state.deleteCandidateCount,
                    LocR.string.insights_delete,
                    locale,
                    Modifier.weight(1f),
                )
                MetricTile(
                    state.favoriteCount,
                    LocR.string.insights_favorites,
                    locale,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    value: Int,
    labelRes: Int,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    PhotidyMetricTile(
        value = PhotidyFormatters.formatNumber(value, locale),
        label = stringResource(labelRes),
        modifier = modifier,
    )
}

/**
 * Three-part donut: organized, flagged for deletion and not reviewed yet. The sweep grows in
 * once on entry, and the legend keeps the numbers readable without relying on colour alone.
 */
@Composable
private fun CompositionCard(
    state: InsightsSummary,
    locale: Locale,
) {
    val colors = Theme.colors
    val remaining = (state.totalPhotos - state.organizedCount).coerceAtLeast(0)
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.insights_composition))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.lg),
        ) {
            CompositionRing(
                organized = state.organizedCount,
                flagged = state.deleteCandidateCount,
                total = state.totalPhotos,
                locale = locale,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
            ) {
                LegendRow(
                    color = colors.accent,
                    label = stringResource(LocR.string.insights_organized),
                    value = PhotidyFormatters.formatNumber(state.organizedCount, locale),
                )
                LegendRow(
                    color = colors.danger,
                    label = stringResource(LocR.string.insights_delete),
                    value = PhotidyFormatters.formatNumber(state.deleteCandidateCount, locale),
                )
                LegendRow(
                    color = colors.textTertiary,
                    label = stringResource(LocR.string.insights_remaining),
                    value = PhotidyFormatters.formatNumber(remaining, locale),
                )
            }
        }
    }
}

@Composable
private fun CompositionRing(
    organized: Int,
    flagged: Int,
    total: Int,
    locale: Locale,
) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    val safeTotal = total.coerceAtLeast(1)
    val organizedSweep = organized.toFloat() / safeTotal
    val flaggedSweep = flagged.toFloat() / safeTotal
    val animatedOrganized by animateFloatAsState(
        targetValue = organizedSweep,
        animationSpec = tween(durationMillis = if (animations) 780 else 0),
        label = "ring-organized",
    )
    val animatedFlagged by animateFloatAsState(
        targetValue = flaggedSweep,
        animationSpec = tween(
            durationMillis = if (animations) 780 else 0,
            delayMillis = if (animations) 120 else 0,
        ),
        label = "ring-flagged",
    )
    val percentage = PhotidyFormatters.formatPercentage(
        if (total == 0) 0f else organized.toFloat() / total,
        locale,
    )

    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val style = Stroke(width = stroke, cap = StrokeCap.Butt)

            drawArc(
                color = colors.glassBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * animatedOrganized.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
            drawArc(
                color = colors.danger,
                startAngle = -90f + 360f * animatedOrganized,
                sweepAngle = 360f * animatedFlagged.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentage,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(LocR.string.insights_completion),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Theme.colors.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Theme.colors.textPrimary,
        )
    }
}

@Composable
private fun SessionCard(
    state: InsightsSummary,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val session = state.session
    PhotidySurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.insights_session))
        if (session == null) {
            Text(
                text = stringResource(LocR.string.insights_no_session),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            SessionRow(
                label = stringResource(
                    LocR.string.insights_session_acted,
                    session.reviewed,
                    session.total,
                ),
                value = PhotidyFormatters.formatPercentage(
                    if (session.total == 0) 0f else session.reviewed.toFloat() / session.total,
                    locale,
                ),
            )
            PhotidyProgressLine(
                progress = if (session.total == 0) 0f else {
                    session.reviewed.toFloat() / session.total
                },
            )
            SessionRow(
                label = stringResource(LocR.string.insights_session_delete, session.markedForDeletion),
                value = null,
            )
            SessionRow(
                label = stringResource(LocR.string.insights_session_favorites, session.favorites),
                value = null,
            )
        }
    }
}

@Composable
private fun SessionRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = Theme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun MonthlyCard(
    state: InsightsSummary,
    locale: Locale,
) {
    val colors = Theme.colors
    val maxCount = state.monthly.maxOfOrNull { it.count } ?: 0
    val peak = state.monthly.maxByOrNull { it.count }?.takeIf { it.count > 0 }
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(
            title = stringResource(LocR.string.insights_monthly),
            subtitle = peak?.let {
                stringResource(
                    LocR.string.insights_monthly_peak,
                    PhotidyFormatters.formatMonthShort(it.year, it.month, locale),
                    it.count,
                )
            },
        )
        if (maxCount == 0) {
            Text(
                text = stringResource(LocR.string.insights_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val currentMonth = YearMonth.now()
                state.monthly.forEachIndexed { index, month ->
                    val isCurrent =
                        month.year == currentMonth.year && month.month == currentMonth.monthValue
                    MonthBar(
                        month = month,
                        maxCount = maxCount,
                        isCurrent = isCurrent,
                        index = index,
                        locale = locale,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.monthly.forEach { month ->
                    Text(
                        text = PhotidyFormatters.formatMonthShort(month.year, month.month, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = colors.textTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthBar(
    month: MonthCount,
    maxCount: Int,
    isCurrent: Boolean,
    index: Int,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    val target = if (maxCount == 0) 0f else month.count.toFloat() / maxCount
    // A small per-bar delay makes the chart grow like a wave instead of one solid block.
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (animations) 620 else 0,
            delayMillis = if (animations) index * 32 else 0,
        ),
        label = "month-bar",
    )
    val barHeight = (98.dp * animated).coerceAtLeast(if (month.count > 0) 3.dp else 2.dp)
    val brush = if (isCurrent) {
        Brush.verticalGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.62f)))
    } else {
        Brush.verticalGradient(
            listOf(
                colors.accent.copy(alpha = if (month.count > 0) 0.72f else 0.14f),
                colors.accent.copy(alpha = if (month.count > 0) 0.26f else 0.08f),
            ),
        )
    }
    val description = pluralStringResource(
        LocR.plurals.photo_count,
        month.count,
        month.count,
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (month.count > 0) {
            Text(
                text = PhotidyFormatters.formatNumber(month.count, locale),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (isCurrent) colors.accent else colors.textTertiary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(barHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(brush),
        )
    }
}

@Composable
private fun StorageCard(
    state: InsightsSummary,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val maxAlbumBytes = state.largestAlbums.maxOfOrNull { it.bytes } ?: 0L
    PhotidySurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.insights_storage))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            Text(
                text = PhotidyFormatters.formatBytes(state.storageBytes, locale),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(
                    LocR.plurals.insights_videos,
                    state.videoCount,
                    state.videoCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            Text(
                text = pluralStringResource(
                    LocR.plurals.insights_screenshots,
                    state.screenshotCount,
                    state.screenshotCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }

        if (state.largestAlbums.isEmpty()) {
            Text(
                text = stringResource(LocR.string.insights_storage_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            Text(
                text = stringResource(LocR.string.insights_storage_albums),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
            )
            state.largestAlbums.forEach { album ->
                AlbumBar(
                    album = album,
                    maxBytes = maxAlbumBytes,
                    locale = locale,
                )
            }
        }

        PhotidyDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(LocR.string.insights_reclaim_delete),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = PhotidyFormatters.formatBytes(state.reclaimBytes, locale),
                style = MaterialTheme.typography.titleSmall,
                color = colors.danger,
            )
        }
    }
}

@Composable
private fun AlbumBar(
    album: AlbumUsage,
    maxBytes: Long,
    locale: Locale,
) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    val fraction = if (maxBytes <= 0) 0f else (album.bytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = if (animations) 680 else 0),
        label = "album-bar",
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = album.title ?: stringResource(LocR.string.insights_unknown_album),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = PhotidyFormatters.formatBytes(album.bytes, locale),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(colors.glassBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.accent, colors.accent.copy(alpha = 0.55f)),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun DuplicatesCard(
    state: InsightsSummary,
    locale: Locale,
    onOpenDuplicates: () -> Unit,
) {
    val colors = Theme.colors
    val hasGroups = state.duplicateGroupCount > 0
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
        onClick = if (hasGroups) onOpenDuplicates else null,
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.insights_duplicates))
        if (hasGroups) {
            Text(
                text = stringResource(
                    LocR.string.insights_duplicates_groups,
                    state.duplicateGroupCount,
                    PhotidyFormatters.formatBytes(state.duplicateReclaimBytes, locale),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(LocR.string.insights_duplicates_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            PhotidyTextButton(
                text = stringResource(LocR.string.insights_duplicates_action),
                onClick = onOpenDuplicates,
                tint = colors.accent,
            )
        } else {
            Text(
                text = stringResource(LocR.string.insights_duplicates_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        InsightsScreen(
            state = InsightsSummary(
                totalPhotos = 56,
                organizedCount = 21,
                deleteCandidateCount = 7,
                favoriteCount = 4,
                monthly = (0 until 12).map { MonthCount(2026, it + 1, (it * 3) % 7) },
                session = app.photidy.core.model.SessionStats(20, 12, 5, 2),
                storageBytes = 860_000_000,
                videoCount = 4,
                screenshotCount = 9,
                largestAlbums = listOf(
                    AlbumUsage("camera", "Camera", 30, 420_000_000),
                    AlbumUsage("travel", "Travel", 12, 260_000_000),
                    AlbumUsage("chat", "Screenshots", 8, 90_000_000),
                ),
                duplicateGroupCount = 3,
                duplicateReclaimBytes = 24_000_000,
                reclaimBytes = 48_000_000,
            ),
            onBack = {},
            onOpenDuplicates = {},
        )
    }
}
