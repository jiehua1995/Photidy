package app.photidy.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.photidy.core.model.AnimationStyle

object PhotidySpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 48.dp
}

object PhotidyRadius {
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 22.dp
    val xl: Dp = 28.dp
    val xxl: Dp = 36.dp
    const val pill: Float = 50f
}

object PhotidySizes {
    val touchTarget: Dp = 48.dp
    val iconSmall: Dp = 18.dp
    val icon: Dp = 22.dp
    val iconLarge: Dp = 26.dp
    val contentMaxWidth: Dp = 640.dp
    val panelMaxWidth: Dp = 960.dp
    val photoCorner: Dp = 26.dp
    val topBarHeight: Dp = 56.dp
}

@Immutable
data class PhotidyColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val cloudPrimary: Color,
    val cloudSecondary: Color,
    val cloudTertiary: Color,
    val scrim: Color,
    val shadow: Color,
) {
    /**
     * Scales the glass tint relative to its base opacity. Replacing the alpha outright would turn
     * the 9% dark-mode glass into an opaque white card, so every glass fill goes through here.
     */
    fun glass(opacityScale: Float): Color =
        glassSurface.copy(alpha = (glassSurface.alpha * opacityScale).coerceIn(0f, 1f))
}

val PhotidyLightColors = PhotidyColors(
    isDark = false,
    background = Color(0xFFF7F5F0),
    backgroundSecondary = Color(0xFFEFEBE3),
    surface = Color(0xFFFCFAF6),
    glassSurface = Color(0x99FFFFFF),
    glassBorder = Color(0x14000000),
    textPrimary = Color(0xFF20211E),
    textSecondary = Color(0xFF6C6A63),
    textTertiary = Color(0xFF9B998F),
    accent = Color(0xFF5F7C6C),
    accentSoft = Color(0xFFDDE7DE),
    onAccent = Color(0xFFFDFDFB),
    danger = Color(0xFFB4695D),
    dangerSoft = Color(0xFFF1DDD8),
    success = Color(0xFF5C7D69),
    cloudPrimary = Color(0xFFDCE7F2),
    cloudSecondary = Color(0xFFE8E1F1),
    cloudTertiary = Color(0xFFEDEAE2),
    scrim = Color(0xFF141511),
    shadow = Color(0xFF3A352C),
)

val PhotidyDarkColors = PhotidyColors(
    isDark = true,
    background = Color(0xFF101210),
    backgroundSecondary = Color(0xFF161815),
    surface = Color(0xFF1B1E1B),
    glassSurface = Color(0x17FFFFFF),
    glassBorder = Color(0x22FFFFFF),
    textPrimary = Color(0xFFF3F1EB),
    textSecondary = Color(0xFFA8A69E),
    textTertiary = Color(0xFF75746C),
    accent = Color(0xFF9FBAA8),
    accentSoft = Color(0xFF28372F),
    onAccent = Color(0xFF101A14),
    danger = Color(0xFFD08D83),
    dangerSoft = Color(0xFF3A2622),
    success = Color(0xFF8FB49D),
    cloudPrimary = Color(0xFF1D2B3A),
    cloudSecondary = Color(0xFF272238),
    cloudTertiary = Color(0xFF23261F),
    scrim = Color(0xFF000000),
    shadow = Color(0xFF000000),
)

@Immutable
data class PhotidyMotion(
    val durationShort: Int,
    val durationMedium: Int,
    val durationLong: Int,
    val springGentle: SpringSpec<Float>,
    val springExpressive: SpringSpec<Float>,
    val easingStandard: Easing,
)

private val standardEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

fun photidyMotion(style: AnimationStyle): PhotidyMotion = when (style) {
    AnimationStyle.GENTLE -> PhotidyMotion(
        durationShort = 260,
        durationMedium = 420,
        durationLong = 640,
        springGentle = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
        springExpressive = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        easingStandard = standardEasing,
    )

    AnimationStyle.NATURAL -> PhotidyMotion(
        durationShort = 180,
        durationMedium = 300,
        durationLong = 460,
        springGentle = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        springExpressive = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMedium),
        easingStandard = standardEasing,
    )

    AnimationStyle.LIVELY -> PhotidyMotion(
        durationShort = 120,
        durationMedium = 220,
        durationLong = 340,
        springGentle = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        springExpressive = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessHigh),
        easingStandard = standardEasing,
    )
}
