package app.photidy.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.designsystem.artwork.PhotoImage
import app.photidy.core.designsystem.components.PhotidyEmptyState
import app.photidy.core.designsystem.components.PhotidyIconButton
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.PhotidyFormatters
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.PhotoWithState

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        state = state,
        onBack = onBack,
        onOpenPhoto = onOpenPhoto,
        onQueryChange = viewModel::updateQuery,
        onClearQuery = viewModel::clearQuery,
        modifier = modifier,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PhotidySpacing.xs,
                        end = PhotidySpacing.lg,
                        top = PhotidySpacing.xs,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhotidyIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(LocR.string.cd_back),
                    onClick = onBack,
                )
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    onClear = onClearQuery,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = PhotidySpacing.xs),
                )
            }

            when {
                state.query.isBlank() -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.search_hint_title),
                        message = stringResource(LocR.string.search_hint_message),
                        icon = Icons.Filled.Search,
                    )
                }

                state.results.isEmpty() -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotidyEmptyState(
                        title = stringResource(LocR.string.search_empty_title),
                        message = stringResource(LocR.string.search_empty_message),
                        icon = Icons.Filled.Search,
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (expanded) PhotidySpacing.xxl else PhotidySpacing.lg,
                            vertical = PhotidySpacing.sm,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = pluralStringResource(
                            LocR.plurals.photo_count,
                            state.results.size,
                            state.results.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .widthIn(max = 1100.dp)
                            .fillMaxWidth()
                            .padding(vertical = PhotidySpacing.xs),
                    )
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
                        items(state.results, key = { it.photo.id }) { item ->
                            SearchTile(
                                item = item,
                                onClick = { onOpenPhoto(item.photo.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(24.dp)

    // The field is the only purpose of this screen, so open the keyboard right away.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(colors.glass(1f), shape)
            .border(1.dp, colors.glassBorder, shape)
            .padding(horizontal = PhotidySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(PhotidySpacing.xs))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(LocR.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(LocR.string.cd_clear_search),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchTile(
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
