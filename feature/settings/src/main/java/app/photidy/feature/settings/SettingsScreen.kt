package app.photidy.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photidy.core.data.AiModelState
import app.photidy.core.data.AiScanProgress
import app.photidy.core.data.LibraryState
import app.photidy.core.designsystem.components.PhotidyBadge
import app.photidy.core.designsystem.components.PhotidyBadgeTone
import app.photidy.core.designsystem.components.PhotidyChip
import app.photidy.core.designsystem.components.PhotidyDivider
import app.photidy.core.designsystem.components.PhotidyProgressLine
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.components.PhotidySectionHeader
import app.photidy.core.designsystem.components.PhotidySegment
import app.photidy.core.designsystem.components.PhotidySegmentedControl
import app.photidy.core.designsystem.components.PhotidySettingRow
import app.photidy.core.designsystem.components.PhotidySlider
import app.photidy.core.designsystem.components.PhotidySurface
import app.photidy.core.designsystem.components.PhotidySwitch
import app.photidy.core.designsystem.components.PhotidyTextButton
import app.photidy.core.designsystem.components.PhotidyTopBar
import app.photidy.core.designsystem.theme.PhotidySpacing
import app.photidy.core.designsystem.theme.PhotidyTheme as Theme
import app.photidy.core.designsystem.theme.PhotidyWidthClass
import app.photidy.core.designsystem.theme.rememberPhotidyWidthClass
import app.photidy.core.localization.R as LocR
import app.photidy.core.model.AnimationStyle
import app.photidy.core.model.AppLanguage
import app.photidy.core.model.AppSettings
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.LibraryMode
import app.photidy.core.model.SwipeDirection
import app.photidy.core.model.ThemeMode
import app.photidy.core.model.VisualStyle
import app.photidy.core.platform.PhotoAccess
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    appVersion: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    val aiModel by viewModel.aiModel.collectAsStateWithLifecycle()
    val aiProgress by viewModel.aiProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (PhotoAccess.hasAnyAccess(context)) {
            viewModel.selectLibraryMode(LibraryMode.DEVICE)
        } else {
            viewModel.refreshLibrary()
        }
    }

    SettingsScreen(
        settings = settings,
        library = library,
        aiModel = aiModel,
        aiProgress = aiProgress,
        appVersion = appVersion,
        onBack = onBack,
        onThemeSelected = viewModel::setThemeMode,
        onStyleSelected = viewModel::setVisualStyle,
        onIntensityChanged = viewModel::setEffectIntensity,
        onAnimationStyleSelected = viewModel::setAnimationStyle,
        onAnimationsToggled = viewModel::setAnimationsEnabled,
        onHapticsToggled = viewModel::setHapticsEnabled,
        onGestureSelected = viewModel::setGesture,
        onResetGestures = viewModel::resetGestures,
        onLanguageSelected = viewModel::setLanguage,
        onLibraryModeSelected = viewModel::selectLibraryMode,
        onRefreshLibrary = viewModel::refreshLibrary,
        onRequestAccess = { permissionLauncher.launch(PhotoAccess.requestPermissions()) },
        onAiToggled = viewModel::setAiRecognition,
        onDownloadAiModel = viewModel::downloadAiModel,
        onDeleteAiModel = viewModel::deleteAiModel,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    library: LibraryState,
    aiModel: AiModelState,
    aiProgress: AiScanProgress,
    appVersion: String,
    onBack: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onStyleSelected: (VisualStyle) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onAnimationStyleSelected: (AnimationStyle) -> Unit,
    onAnimationsToggled: (Boolean) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
    onGestureSelected: (SwipeDirection, GestureAction) -> Unit,
    onResetGestures: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLibraryModeSelected: (LibraryMode) -> Unit,
    onRefreshLibrary: () -> Unit,
    onRequestAccess: () -> Unit,
    onAiToggled: (Boolean) -> Unit,
    onDownloadAiModel: () -> Unit,
    onDeleteAiModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberPhotidyWidthClass()
    val expanded = widthClass == PhotidyWidthClass.EXPANDED

    PhotidyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            PhotidyTopBar(
                title = stringResource(LocR.string.settings_title),
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
            ) {
                if (expanded) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.xxl),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            LibraryCard(
                                library = library,
                                onModeSelected = onLibraryModeSelected,
                                onRefresh = onRefreshLibrary,
                                onRequestAccess = onRequestAccess,
                            )
                            AiCard(
                                enabled = settings.aiRecognitionEnabled,
                                state = aiModel,
                                progress = aiProgress,
                                onToggle = onAiToggled,
                                onDownload = onDownloadAiModel,
                                onDelete = onDeleteAiModel,
                            )
                            AppearanceCard(
                                settings = settings,
                                onThemeSelected = onThemeSelected,
                                onStyleSelected = onStyleSelected,
                                onIntensityChanged = onIntensityChanged,
                                onAnimationStyleSelected = onAnimationStyleSelected,
                                onAnimationsToggled = onAnimationsToggled,
                                onHapticsToggled = onHapticsToggled,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                        ) {
                            GesturesCard(
                                gestures = settings.gestures,
                                hapticsEnabled = settings.hapticsEnabled,
                                onGestureSelected = onGestureSelected,
                                onResetGestures = onResetGestures,
                            )
                            LanguageCard(
                                settings = settings,
                                onLanguageSelected = onLanguageSelected,
                            )
                            AboutCard(appVersion = appVersion)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PhotidySpacing.md),
                    ) {
                        LibraryCard(
                            library = library,
                            onModeSelected = onLibraryModeSelected,
                            onRefresh = onRefreshLibrary,
                            onRequestAccess = onRequestAccess,
                        )
                        AiCard(
                            enabled = settings.aiRecognitionEnabled,
                            state = aiModel,
                            progress = aiProgress,
                            onToggle = onAiToggled,
                            onDownload = onDownloadAiModel,
                            onDelete = onDeleteAiModel,
                        )
                        AppearanceCard(
                            settings = settings,
                            onThemeSelected = onThemeSelected,
                            onStyleSelected = onStyleSelected,
                            onIntensityChanged = onIntensityChanged,
                            onAnimationStyleSelected = onAnimationStyleSelected,
                            onAnimationsToggled = onAnimationsToggled,
                            onHapticsToggled = onHapticsToggled,
                        )
                        GesturesCard(
                            gestures = settings.gestures,
                            hapticsEnabled = settings.hapticsEnabled,
                            onGestureSelected = onGestureSelected,
                            onResetGestures = onResetGestures,
                        )
                        LanguageCard(
                            settings = settings,
                            onLanguageSelected = onLanguageSelected,
                        )
                        AboutCard(appVersion = appVersion)
                    }
                }
                Spacer(Modifier.height(PhotidySpacing.xxxl))
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: LibraryState,
    onModeSelected: (LibraryMode) -> Unit,
    onRefresh: () -> Unit,
    onRequestAccess: () -> Unit,
) {
    val colors = Theme.colors
    val deviceSubtitle = when {
        !library.hasAccess -> stringResource(LocR.string.settings_library_no_access)
        library.isScanning -> stringResource(LocR.string.settings_library_scanning)
        !library.hasFullAccess -> pluralStringResource(
            LocR.plurals.device_photos_found_limited,
            library.devicePhotoCount,
            library.devicePhotoCount,
        )
        else -> pluralStringResource(
            LocR.plurals.device_photos_found,
            library.devicePhotoCount,
            library.devicePhotoCount,
        )
    }
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.settings_library))
        Spacer(Modifier.height(PhotidySpacing.xs))
        LibraryRow(
            title = stringResource(LocR.string.settings_library_device),
            subtitle = deviceSubtitle,
            selected = library.mode == LibraryMode.DEVICE,
            onClick = { onModeSelected(LibraryMode.DEVICE) },
        )
        LibraryRow(
            title = stringResource(LocR.string.settings_library_demo),
            subtitle = stringResource(LocR.string.settings_library_demo_subtitle),
            selected = library.mode == LibraryMode.DEMO,
            onClick = { onModeSelected(LibraryMode.DEMO) },
        )
        if (library.mode == LibraryMode.DEVICE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (library.hasAccess) {
                    PhotidyTextButton(
                        text = stringResource(LocR.string.settings_library_rescan),
                        onClick = onRefresh,
                        enabled = !library.isScanning,
                        tint = colors.accent,
                    )
                }
                if (!library.hasFullAccess) {
                    PhotidyTextButton(
                        text = stringResource(LocR.string.settings_library_allow),
                        onClick = onRequestAccess,
                        tint = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = PhotidySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(LocR.string.cd_selected),
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AiCard(
    enabled: Boolean,
    state: AiModelState,
    progress: AiScanProgress,
    onToggle: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.settings_ai))
        PhotidySettingRow(
            title = stringResource(LocR.string.settings_ai_toggle),
            subtitle = stringResource(LocR.string.settings_ai_toggle_desc),
            trailing = {
                PhotidySwitch(checked = enabled, onCheckedChange = onToggle)
            },
        )
        when {
            !enabled -> Text(
                text = stringResource(LocR.string.settings_ai_off_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )

            state is AiModelState.Downloading -> {
                Text(
                    text = stringResource(
                        LocR.string.settings_ai_downloading,
                        (state.progress * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(PhotidySpacing.xs))
                PhotidyProgressLine(progress = state.progress)
            }

            state is AiModelState.Ready -> {
                AiStatusRow(
                    text = stringResource(LocR.string.settings_ai_ready),
                    actionLabel = stringResource(LocR.string.settings_ai_delete),
                    onAction = onDelete,
                )
                if (progress.running && progress.total > 0) {
                    Spacer(Modifier.height(PhotidySpacing.xs))
                    Text(
                        text = stringResource(
                            LocR.string.settings_ai_scanning,
                            progress.done,
                            progress.total,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(PhotidySpacing.xxs))
                    PhotidyProgressLine(
                        progress = progress.done.toFloat() / progress.total,
                    )
                }
            }

            state is AiModelState.Failed -> AiStatusRow(
                text = stringResource(LocR.string.settings_ai_failed),
                actionLabel = stringResource(LocR.string.settings_ai_retry),
                onAction = onDownload,
                tint = colors.danger,
            )

            else -> AiStatusRow(
                text = stringResource(LocR.string.settings_ai_missing),
                actionLabel = stringResource(LocR.string.settings_ai_download),
                onAction = onDownload,
            )
        }
    }
}

@Composable
private fun AiStatusRow(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    tint: Color = Theme.colors.textSecondary,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            modifier = Modifier.weight(1f),
        )
        PhotidyTextButton(
            text = actionLabel,
            onClick = onAction,
            tint = Theme.colors.accent,
        )
    }
}

@Composable
private fun AppearanceCard(
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onStyleSelected: (VisualStyle) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onAnimationStyleSelected: (AnimationStyle) -> Unit,
    onAnimationsToggled: (Boolean) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.settings_appearance))
        Spacer(Modifier.height(PhotidySpacing.xs))

        Text(
            text = stringResource(LocR.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        PhotidySegmentedControl(
            options = listOf(
                PhotidySegment(ThemeMode.SYSTEM, stringResource(LocR.string.settings_theme_system)),
                PhotidySegment(ThemeMode.LIGHT, stringResource(LocR.string.settings_theme_light)),
                PhotidySegment(ThemeMode.DARK, stringResource(LocR.string.settings_theme_dark)),
            ),
            selected = settings.themeMode,
            onSelect = onThemeSelected,
        )

        Spacer(Modifier.height(PhotidySpacing.sm))
        Text(
            text = stringResource(LocR.string.settings_style),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        VisualStyle.entries.forEach { style ->
            StyleOptionRow(
                style = style,
                selected = settings.visualStyle == style,
                onClick = { onStyleSelected(style) },
            )
        }

        PhotidyDivider(Modifier.padding(vertical = PhotidySpacing.xs))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(LocR.string.settings_intensity),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    LocR.string.settings_intensity_value,
                    (settings.effectIntensity * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        PhotidySlider(
            value = settings.effectIntensity,
            onValueChange = onIntensityChanged,
        )

        PhotidyDivider(Modifier.padding(vertical = PhotidySpacing.xs))

        Text(
            text = stringResource(LocR.string.settings_animation),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        PhotidySegmentedControl(
            options = listOf(
                PhotidySegment(AnimationStyle.GENTLE, stringResource(LocR.string.settings_animation_gentle)),
                PhotidySegment(AnimationStyle.NATURAL, stringResource(LocR.string.settings_animation_natural)),
                PhotidySegment(AnimationStyle.LIVELY, stringResource(LocR.string.settings_animation_lively)),
            ),
            selected = settings.animationStyle,
            onSelect = onAnimationStyleSelected,
        )

        PhotidySettingRow(
            title = stringResource(LocR.string.settings_animations),
            subtitle = stringResource(LocR.string.settings_animations_desc),
            trailing = {
                PhotidySwitch(
                    checked = settings.animationsEnabled,
                    onCheckedChange = onAnimationsToggled,
                )
            },
        )
        PhotidySettingRow(
            title = stringResource(LocR.string.settings_haptics),
            subtitle = stringResource(LocR.string.settings_haptics_desc),
            trailing = {
                PhotidySwitch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsToggled,
                )
            },
        )
    }
}

@Composable
private fun StyleOptionRow(
    style: VisualStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    val title = when (style) {
        VisualStyle.MINIMAL -> stringResource(LocR.string.settings_style_minimal)
        VisualStyle.LIQUID_GLASS -> stringResource(LocR.string.settings_style_glass)
        VisualStyle.FLUID_CLOUD -> stringResource(LocR.string.settings_style_cloud)
    }
    val description = when (style) {
        VisualStyle.MINIMAL -> stringResource(LocR.string.settings_style_minimal_desc)
        VisualStyle.LIQUID_GLASS -> stringResource(LocR.string.settings_style_glass_desc)
        VisualStyle.FLUID_CLOUD -> stringResource(LocR.string.settings_style_cloud_desc)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = PhotidySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StylePreview(
            style = style,
            selected = selected,
            modifier = Modifier.size(width = 56.dp, height = 40.dp),
        )
        Spacer(Modifier.width(PhotidySpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StylePreview(
    style: VisualStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val shape = RoundedCornerShape(11.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.glassBorder,
                shape = shape,
            ),
    ) {
        when (style) {
            VisualStyle.MINIMAL -> {
                drawRect(color = colors.surface)
                drawRect(
                    color = colors.backgroundSecondary,
                    topLeft = Offset(size.width * 0.2f, size.height * 0.62f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.09f),
                )
            }

            VisualStyle.LIQUID_GLASS -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.glassSurface, colors.accentSoft.copy(alpha = 0.6f)),
                    ),
                )
                drawCircle(
                    color = colors.accent.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.68f, size.height * 0.34f),
                )
            }

            VisualStyle.FLUID_CLOUD -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.cloudPrimary, colors.cloudSecondary),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.cloudTertiary, Color.Transparent),
                        center = Offset(size.width * 0.3f, size.height * 0.6f),
                        radius = size.minDimension * 0.8f,
                    ),
                    radius = size.minDimension * 0.8f,
                    center = Offset(size.width * 0.3f, size.height * 0.6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GesturesCard(
    gestures: GestureBindings,
    hapticsEnabled: Boolean,
    onGestureSelected: (SwipeDirection, GestureAction) -> Unit,
    onResetGestures: () -> Unit,
) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(
            title = stringResource(LocR.string.settings_gestures_title),
            subtitle = stringResource(LocR.string.settings_gestures_desc),
        )
        Spacer(Modifier.height(PhotidySpacing.xs))

        val directions = listOf(
            SwipeDirection.LEFT,
            SwipeDirection.RIGHT,
            SwipeDirection.UP,
            SwipeDirection.DOWN,
        )
        directions.forEachIndexed { index, direction ->
            GestureEditorRow(
                direction = direction,
                action = gestures.actionFor(direction),
                onActionSelected = { action -> onGestureSelected(direction, action) },
            )
            if (index != directions.lastIndex) {
                PhotidyDivider()
            }
        }

        val usesAlbum = directions.any { gestures.actionFor(it).requiresAlbum }
        if (usesAlbum) {
            Text(
                text = stringResource(LocR.string.settings_gestures_album_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            PhotidyTextButton(
                text = stringResource(LocR.string.settings_gestures_reset),
                onClick = onResetGestures,
                tint = colors.accent,
            )
        }

        Spacer(Modifier.height(PhotidySpacing.sm))
        PhotidySectionHeader(title = stringResource(LocR.string.settings_gesture_demo_title))
        GestureDemoCard(
            bindings = gestures,
            hapticsEnabled = hapticsEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GestureEditorRow(
    direction: SwipeDirection,
    action: GestureAction,
    onActionSelected: (GestureAction) -> Unit,
) {
    val colors = Theme.colors
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = PhotidySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = directionIcon(direction),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(PhotidySpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(directionLabelRes(direction)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(actionLabelRes(action)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            FlowRow(
                modifier = Modifier.padding(bottom = PhotidySpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PhotidySpacing.sm),
            ) {
                GestureAction.entries.forEach { candidate ->
                    PhotidyChip(
                        label = stringResource(actionLabelRes(candidate)),
                        selected = candidate == action,
                        onClick = {
                            onActionSelected(candidate)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    settings: AppSettings,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val colors = Theme.colors
    var expanded by remember { mutableStateOf(false) }
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.settings_language))
        Spacer(Modifier.height(PhotidySpacing.xs))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(vertical = PhotidySpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = languageLabel(settings.language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = colors.textTertiary,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.surface,
            ) {
                AppLanguage.entries.forEach { language ->
                    val selected = language == settings.language
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = languageLabel(language),
                                color = if (selected) colors.accent else colors.textPrimary,
                            )
                        },
                        trailingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(LocR.string.cd_selected),
                                    tint = colors.accent,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            expanded = false
                            onLanguageSelected(language)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.SYSTEM -> LocR.string.settings_language_system
        AppLanguage.CHINESE -> LocR.string.settings_language_zh
        AppLanguage.CHINESE_TRADITIONAL -> LocR.string.settings_language_zh_tw
        AppLanguage.ENGLISH -> LocR.string.settings_language_en
        AppLanguage.GERMAN -> LocR.string.settings_language_de
        AppLanguage.JAPANESE -> LocR.string.settings_language_ja
        AppLanguage.KOREAN -> LocR.string.settings_language_ko
        AppLanguage.SPANISH -> LocR.string.settings_language_es
        AppLanguage.FRENCH -> LocR.string.settings_language_fr
        AppLanguage.ITALIAN -> LocR.string.settings_language_it
    },
)

@Composable
private fun AboutCard(appVersion: String) {
    val colors = Theme.colors
    PhotidySurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PhotidySpacing.lg),
    ) {
        PhotidySectionHeader(title = stringResource(LocR.string.settings_about))
        Spacer(Modifier.height(PhotidySpacing.xs))
        Text(
            text = stringResource(LocR.string.brand_name),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(LocR.string.settings_version, appVersion),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(PhotidySpacing.sm))
        Text(
            text = stringResource(LocR.string.settings_privacy_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(LocR.string.settings_privacy_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(PhotidySpacing.sm))
        Text(
            text = stringResource(LocR.string.settings_developer_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(LocR.string.settings_developer_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(PhotidySpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(LocR.string.settings_support_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(LocR.string.settings_support_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            PhotidyBadge(
                text = stringResource(LocR.string.common_coming_soon),
                tone = PhotidyBadgeTone.ACCENT,
            )
        }
    }
}

private fun directionLabelRes(direction: SwipeDirection): Int = when (direction) {
    SwipeDirection.LEFT -> LocR.string.gesture_left
    SwipeDirection.RIGHT -> LocR.string.gesture_right
    SwipeDirection.UP -> LocR.string.gesture_up
    SwipeDirection.DOWN -> LocR.string.gesture_down
}

internal fun directionIcon(direction: SwipeDirection): ImageVector = when (direction) {
    SwipeDirection.LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    SwipeDirection.RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    SwipeDirection.UP -> Icons.Filled.KeyboardArrowUp
    SwipeDirection.DOWN -> Icons.Filled.KeyboardArrowDown
}
internal fun actionLabelRes(action: GestureAction): Int = when (action) {
    GestureAction.ADD_TO_DELETE_LIST -> LocR.string.organizer_action_delete
    GestureAction.KEEP -> LocR.string.organizer_action_keep
    GestureAction.FAVORITE -> LocR.string.organizer_action_favorite
    GestureAction.ADD_TO_ALBUM -> LocR.string.organizer_action_album
    GestureAction.SKIP -> LocR.string.organizer_action_skip
    GestureAction.NONE -> LocR.string.organizer_action_none
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    Theme(settings = app.photidy.core.model.AppSettings()) {
        SettingsScreen(
            settings = AppSettings(),
            library = LibraryState(),
            aiModel = AiModelState.NotDownloaded,
            aiProgress = AiScanProgress(),
            appVersion = "0.1.0",
            onBack = {},
            onThemeSelected = {},
            onStyleSelected = {},
            onIntensityChanged = {},
            onAnimationStyleSelected = {},
            onAnimationsToggled = {},
            onHapticsToggled = {},
            onGestureSelected = { _, _ -> },
            onResetGestures = {},
            onLanguageSelected = {},
            onLibraryModeSelected = {},
            onRefreshLibrary = {},
            onRequestAccess = {},
            onAiToggled = {},
            onDownloadAiModel = {},
            onDeleteAiModel = {},
        )
    }
}
