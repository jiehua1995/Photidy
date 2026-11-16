package app.photidy.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.photidy.core.model.AnimationStyle
import app.photidy.core.model.AppSettings
import app.photidy.core.model.ThemeMode
import app.photidy.core.model.VisualStyle
import app.photidy.core.platform.DeviceEnvironment

val LocalPhotidyColors = staticCompositionLocalOf { PhotidyLightColors }
val LocalVisualStyle = staticCompositionLocalOf { VisualStyle.MINIMAL }
val LocalEffectIntensity = staticCompositionLocalOf { 0.6f }
val LocalPhotidyMotion = staticCompositionLocalOf { photidyMotion(AnimationStyle.NATURAL) }
val LocalAnimationsEnabled = staticCompositionLocalOf { true }
val LocalReducedEffects = staticCompositionLocalOf { false }

/**
 * Photidy's design system entry point. Material 3 is used as a component substrate,
 * while all visual decisions come from Photidy's own tokens.
 */
@Composable
fun PhotidyTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) PhotidyDarkColors else PhotidyLightColors
    val reducedEffects = remember(context) { DeviceEnvironment.shouldReduceEffects(context) }
    val reduceMotion = remember(context) { DeviceEnvironment.isReduceMotionEnabled(context) }
    val animationsEnabled = settings.animationsEnabled && !reduceMotion
    val intensity = if (reducedEffects) settings.effectIntensity * 0.35f else settings.effectIntensity
    val motion = remember(settings.animationStyle) { photidyMotion(settings.animationStyle) }

    val materialScheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.backgroundSecondary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.glassBorder,
            error = colors.danger,
            onError = colors.onAccent,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.backgroundSecondary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.glassBorder,
            error = colors.danger,
            onError = colors.onAccent,
        )
    }

    val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.xs),
        small = androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.sm),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.md),
        large = androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.lg),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(PhotidyRadius.xl),
    )

    CompositionLocalProvider(
        LocalPhotidyColors provides colors,
        LocalVisualStyle provides settings.visualStyle,
        LocalEffectIntensity provides intensity,
        LocalPhotidyMotion provides motion,
        LocalAnimationsEnabled provides animationsEnabled,
        LocalReducedEffects provides reducedEffects,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = PhotidyTypography,
            shapes = shapes,
            content = content,
        )
    }
}

object PhotidyTheme {
    val colors: PhotidyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPhotidyColors.current

    val visualStyle: VisualStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalVisualStyle.current

    val effectIntensity: Float
        @Composable
        @ReadOnlyComposable
        get() = LocalEffectIntensity.current

    val motion: PhotidyMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalPhotidyMotion.current

    val animationsEnabled: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAnimationsEnabled.current
}

private val PhotidyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
    ),
)

@Immutable
enum class PhotidyWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Composable
fun rememberPhotidyWidthClass(): PhotidyWidthClass {
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    return when {
        width < 600 -> PhotidyWidthClass.COMPACT
        width < 840 -> PhotidyWidthClass.MEDIUM
        else -> PhotidyWidthClass.EXPANDED
    }
}
