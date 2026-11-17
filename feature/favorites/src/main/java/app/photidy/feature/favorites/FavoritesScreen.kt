package app.photidy.feature.favorites

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyPrimaryButton
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoWithState
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun FavoritesRoute(
    onBack: (() -> Unit)?,
    onOpenPhoto: (String) -> Unit,
    onSessionStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FavoritesViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    FavoritesScreen(
        state = state,
        onBack = onBack,
        onOpenPhoto = onOpenPhoto,
        onStart = {
            scope.launch {
                if (viewModel.startSession() > 0) {
                    onSessionStarted()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onBack: (() -> Unit)?,
    onOpenPhoto: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass != PhotidyWidthClass.COMPACT
    val locale: Locale = LocalConfiguration.current.locales[0]

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.favorites_title),
                subtitle = pluralStringResource(
                    LocR.plurals.photo_count,
                    state.items.size,
                    state.items.size,
                ),
                onBack = onBack,
            )

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.favorites_empty_title),
                        message = stringResource(LocR.string.favorites_empty_message),
                        icon = Icons.Filled.Favorite,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                    }

                    Spacer(Modifier.height(PhotidySpacing.md))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (expanded) 148.dp else 104.dp),
                        modifier = Modifier
                            .widthIn(max = 1100.dp)
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = PhotidySpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                    ) {
                        items(state.items, key = { it.photo.id }) { item ->
                            FavoriteTile(
                                item = item,
                                onClick = { onOpenPhoto(item.photo.id) },
                            )
                        }
                    }
                }

                PhotidyPrimaryButton(
                    text = pluralStringResource(
                        LocR.plurals.start_with_photos,
                        state.items.size,
                        state.items.size,
                    ),
                    onClick = onStart,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                            vertical = PhotidySpacing.md,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FavoriteTile(
    item: PhotoWithState,
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

@Preview(showBackground = true)
@Composable
private fun FavoritesScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        FavoritesScreen(
            state = FavoritesUiState(isLoading = false),
            onBack = {},
            onOpenPhoto = {},
            onStart = {},
        )
    }
}
