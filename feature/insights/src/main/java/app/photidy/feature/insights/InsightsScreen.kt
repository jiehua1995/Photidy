package app.photidy.feature.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.components.PhotidyMetricTile
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySectionHeader
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.InsightsSummary
import app.photidy.core.model.MonthCount
import java.time.YearMonth
import java.util.Locale

@Composable
fun InsightsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: InsightsViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(state = state, onBack = onBack, modifier = modifier)
}

@Composable
fun InsightsScreen(
    state: InsightsSummary,
    onBack: () -> Unit,
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

                    if (expanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                            CompletionCard(
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
                        CompletionCard(state = state, locale = locale)
                        SessionCard(state = state, locale = locale)
                    }

                    MonthlyCard(state = state, locale = locale)
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
            PhotidyMetricTile(
                value = PhotidyFormatters.formatNumber(state.totalPhotos, locale),
                label = stringResource(LocR.string.insights_total),
                modifier = Modifier.weight(1f),
            )
            PhotidyMetricTile(
                value = PhotidyFormatters.formatNumber(state.organizedCount, locale),
                label = stringResource(LocR.string.insights_organized),
                modifier = Modifier.weight(1f),
            )
            PhotidyMetricTile(
                value = PhotidyFormatters.formatNumber(state.deleteCandidateCount, locale),
                label = stringResource(LocR.string.insights_delete),
                modifier = Modifier.weight(1f),
            )
            PhotidyMetricTile(
                value = PhotidyFormatters.formatNumber(state.favoriteCount, locale),
                label = stringResource(LocR.string.insights_favorites),
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                PhotidyMetricTile(
                    value = PhotidyFormatters.formatNumber(state.totalPhotos, locale),
                    label = stringResource(LocR.string.insights_total),
                    modifier = Modifier.weight(1f),
                )
                PhotidyMetricTile(
                    value = PhotidyFormatters.formatNumber(state.organizedCount, locale),
                    label = stringResource(LocR.string.insights_organized),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md)) {
                PhotidyMetricTile(
                    value = PhotidyFormatters.formatNumber(state.deleteCandidateCount, locale),
                    label = stringResource(LocR.string.insights_delete),
                    modifier = Modifier.weight(1f),
                )
                PhotidyMetricTile(
                    value = PhotidyFormatters.formatNumber(state.favoriteCount, locale),
                    label = stringResource(LocR.string.insights_favorites),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(
    state: InsightsSummary,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(PhotidySpacing.lg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.lg),
        ) {
            CompletionRing(progress = state.organizedRatio, locale = locale)
            Column {
                Text(
                    text = stringResource(LocR.string.insights_completion),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary,
                )
                Text(
                    text = PhotidyFormatters.formatPercentage(state.organizedRatio, locale),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(
                        LocR.string.home_insights_subtitle,
                        state.organizedCount,
                        state.totalPhotos,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CompletionRing(progress: Float, locale: Locale) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (animations) 700 else 0),
        label = "completion-ring",
    )
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            drawArc(
                color = colors.glassBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = PhotidyFormatters.formatPercentage(progress.coerceIn(0f, 1f), locale),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(PhotidySpacing.lg),
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
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.insights_monthly))
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
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val currentMonth = YearMonth.now()
                state.monthly.forEach { month ->
                    val isCurrent = month.year == currentMonth.year && month.month == currentMonth.monthValue
                    MonthBar(
                        month = month,
                        maxCount = maxCount,
                        isCurrent = isCurrent,
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
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val animations = Theme.animationsEnabled
    val target = if (maxCount == 0) 0f else month.count.toFloat() / maxCount
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (animations) 620 else 0),
        label = "month-bar",
    )
    val barHeight = (96.dp * animated).coerceAtLeast(if (month.count > 0) 3.dp else 2.dp)
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
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(barHeight)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (isCurrent) {
                        colors.accent
                    } else {
                        colors.accent.copy(alpha = if (month.count > 0) 0.45f else 0.16f)
                    },
                ),
        )
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
            ),
            onBack = {},
        )
    }
}
