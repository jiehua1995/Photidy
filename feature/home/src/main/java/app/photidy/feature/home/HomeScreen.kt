package app.photidy.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyBadge
import app.photidy.core.designsystem.components.PhotidyIconButton
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyProgressLine
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.LibraryMode
import app.photidy.core.model.PhotoItem
import app.photidy.core.platform.PhotoAccess
import java.time.LocalTime
import java.util.Locale

@Composable
fun HomeRoute(
    onStartOrganizer: () -> Unit,
    onContinueOrganizer: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var accessDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (PhotoAccess.hasAnyAccess(context)) {
            accessDenied = false
            viewModel.useDeviceLibrary()
        } else {
            accessDenied = true
        }
    }

    HomeScreen(
        state = state,
        accessDenied = accessDenied,
        onRequestAccess = { permissionLauncher.launch(PhotoAccess.requestPermissions()) },
        onKeepDemo = viewModel::keepDemoLibrary,
        onStartOrganizer = onStartOrganizer,
        onContinueOrganizer = onContinueOrganizer,
        onOpenReview = onOpenReview,
        onOpenFavorites = onOpenFavorites,
        onOpenInsights = onOpenInsights,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    accessDenied: Boolean,
    onRequestAccess: () -> Unit,
    onKeepDemo: () -> Unit,
    onStartOrganizer: () -> Unit,
    onContinueOrganizer: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass == PhotidyWidthClass.EXPANDED
    val locale: Locale = LocalConfiguration.current.locales[0]
    val activeSession = state.activeSessionRemaining != null
    val hour = remember { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> LocR.string.home_greeting_morning
            hour < 18 -> LocR.string.home_greeting_afternoon
            else -> LocR.string.home_greeting_evening
        },
    )

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .padding(top = PhotidySpacing.lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(LocR.string.brand_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Theme.colors.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    PhotidyIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(LocR.string.cd_settings),
                        onClick = onOpenSettings,
                    )
                }

                Spacer(Modifier.height(PhotidySpacing.xxl))

                if (expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.huge),
                    ) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.lg),
                        ) {
                            GreetingBlock(greeting)
                            if (state.showLibraryPrompt) {
                                LibraryPromptCard(
                                    accessDenied = accessDenied,
                                    onRequestAccess = onRequestAccess,
                                    onKeepDemo = onKeepDemo,
                                )
                            }
                            PrimaryActionCard(
                                state = state,
                                activeSession = activeSession,
                                onStartOrganizer = onStartOrganizer,
                                onContinueOrganizer = onContinueOrganizer,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.85f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            Spacer(Modifier.height(PhotidySpacing.xxl))
                            FavoritesCard(state = state, onClick = onOpenFavorites)
                            ReviewCard(
                                state = state,
                                locale = locale,
                                onClick = onOpenReview,
                            )
                            InsightsCard(state = state, onClick = onOpenInsights)
                            LibrarySummary(state = state, locale = locale)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(PhotidySpacing.lg)) {
                        GreetingBlock(greeting)
                        if (state.showLibraryPrompt) {
                            LibraryPromptCard(
                                accessDenied = accessDenied,
                                onRequestAccess = onRequestAccess,
                                onKeepDemo = onKeepDemo,
                            )
                        }
                        PrimaryActionCard(
                            state = state,
                            activeSession = activeSession,
                            onStartOrganizer = onStartOrganizer,
                            onContinueOrganizer = onContinueOrganizer,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            ReviewCard(
                                state = state,
                                locale = locale,
                                onClick = onOpenReview,
                                modifier = Modifier.weight(1f),
                            )
                            InsightsCard(
                                state = state,
                                onClick = onOpenInsights,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        FavoritesCard(
                            state = state,
                            onClick = onOpenFavorites,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LibrarySummary(state = state, locale = locale)
                    }
                }

                Spacer(Modifier.height(PhotidySpacing.xxxl))
            }
        }
    }
}

@Composable
private fun GreetingBlock(greeting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            color = Theme.colors.textPrimary,
        )
        Text(
            text = stringResource(LocR.string.home_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.textSecondary,
        )
    }
}

@Composable
private fun LibraryPromptCard(
    accessDenied: Boolean,
    onRequestAccess: () -> Unit,
    onKeepDemo: () -> Unit,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        Text(
            text = stringResource(LocR.string.home_library_prompt_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(LocR.string.home_library_prompt_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (accessDenied) {
            Text(
                text = stringResource(LocR.string.home_library_prompt_denied),
                style = MaterialTheme.typography.labelSmall,
                color = colors.danger,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            PhotidyPrimaryButton(
                text = stringResource(LocR.string.home_library_prompt_action),
                onClick = onRequestAccess,
            )
            PhotidyTextButton(
                text = stringResource(LocR.string.home_library_prompt_keep_demo),
                onClick = onKeepDemo,
            )
        }
    }
}

@Composable
private fun PrimaryActionCard(
    state: HomeUiState,
    activeSession: Boolean,
    onStartOrganizer: () -> Unit,
    onContinueOrganizer: () -> Unit,
) {
    val title = if (activeSession) {
        stringResource(LocR.string.home_continue_title)
    } else {
        stringResource(LocR.string.home_start_title)
    }
    val subtitle = if (activeSession) {
        val remaining = state.activeSessionRemaining ?: 0
        pluralStringResource(LocR.plurals.photos_left, remaining, remaining)
    } else if (state.sessionCompleted) {
        stringResource(LocR.string.home_session_complete_subtitle)
    } else {
        stringResource(LocR.string.home_start_subtitle)
    }
    val cta = if (activeSession) {
        stringResource(LocR.string.home_secondary_cta)
    } else {
        stringResource(LocR.string.home_primary_cta)
    }

    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.xs),
            ) {
                if (state.sessionCompleted && !activeSession) {
                    Text(
                        text = stringResource(LocR.string.home_session_complete_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = Theme.colors.accent,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Theme.colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Theme.colors.textSecondary,
                )
                Spacer(Modifier.height(PhotidySpacing.sm))
                PhotidyPrimaryButton(
                    text = cta,
                    onClick = if (activeSession) onContinueOrganizer else onStartOrganizer,
                    enabled = !state.isLoading,
                )
            }
            if (state.previewPhotos.isNotEmpty()) {
                Spacer(Modifier.size(PhotidySpacing.sm))
                PhotoStack(photos = state.previewPhotos)
            }
        }
    }
}

@Composable
private fun PhotoStack(photos: List<PhotoItem>) {
    val colors = Theme.colors
    Box(modifier = Modifier.size(102.dp)) {
        photos.take(3).forEachIndexed { index, photo ->
            val corner = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.Center)
                    .offset(x = ((index - 1) * 12).dp, y = ((index - 1) * 7).dp)
                    .rotate((index - 1) * 5f)
                    .clip(corner)
                    .border(1.dp, colors.glassBorder, corner),
            ) {
                PhotoImage(
                    photo = photo,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FavoritesCard(
    state: HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickCard(
        title = stringResource(LocR.string.home_favorites_title),
        subtitle = pluralStringResource(
            LocR.plurals.photo_count,
            state.favoriteCount,
            state.favoriteCount,
        ),
        icon = Icons.Filled.Favorite,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ReviewCard(
    state: HomeUiState,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (state.deleteCount == 0) {
        stringResource(LocR.string.home_review_subtitle_empty)
    } else {
        pluralStringResource(
            LocR.plurals.photos_with_size,
            state.deleteCount,
            state.deleteCount,
            PhotidyFormatters.formatBytes(state.deleteBytes, locale),
        )
    }
    QuickCard(
        title = stringResource(LocR.string.home_review_title),
        subtitle = subtitle,
        icon = Icons.Filled.Delete,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun InsightsCard(
    state: HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickCard(
        title = stringResource(LocR.string.home_insights_title),
        subtitle = stringResource(
            LocR.string.home_insights_subtitle,
            state.organizedCount,
            state.totalPhotos,
        ),
        icon = Icons.Filled.DateRange,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = modifier,
        contentPadding = PaddingValues(PhotidySpacing.lg),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibrarySummary(state: HomeUiState, locale: Locale) {
    val colors = Theme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PhotidySpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(LocR.string.home_library_label),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
            )
            Spacer(Modifier.width(PhotidySpacing.sm))
            PhotidyBadge(
                text = stringResource(
                    if (state.library.mode == LibraryMode.DEVICE) {
                        LocR.string.home_library_source_device
                    } else {
                        LocR.string.home_library_source_demo
                    },
                ),
            )
            Spacer(Modifier.weight(1f))
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
        PhotidyProgressLine(
            progress = if (state.totalPhotos == 0) 0f else {
                state.organizedCount.toFloat() / state.totalPhotos
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                totalPhotos = 24,
                organizedCount = 11,
                favoriteCount = 3,
                deleteCount = 4,
                deleteBytes = 18_400_000,
                activeSessionRemaining = 9,
            ),
            accessDenied = false,
            onRequestAccess = {},
            onKeepDemo = {},
            onStartOrganizer = {},
            onContinueOrganizer = {},
            onOpenReview = {},
            onOpenFavorites = {},
            onOpenInsights = {},
            onOpenSettings = {},
        )
    }
}
