package app.photidy.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
enum class VisualStyle {
    MINIMAL,
    LIQUID_GLASS,
    FLUID_CLOUD,
}

@Serializable
enum class AnimationStyle {
    GENTLE,
    NATURAL,
    LIVELY,
}

@Serializable
enum class AppLanguage {
    SYSTEM,
    CHINESE,
    CHINESE_TRADITIONAL,
    ENGLISH,
    GERMAN,
    JAPANESE,
    KOREAN,
    SPANISH,
    FRENCH,
    ITALIAN,
}

/** Where the photo list comes from. Device access is permission-gated by the host app. */
@Serializable
enum class LibraryMode {
    DEMO,
    DEVICE,
}

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val visualStyle: VisualStyle = VisualStyle.MINIMAL,
    /** 0f..1f. Controls cloud opacity, glass depth and glow strength. */
    val effectIntensity: Float = 0.6f,
    val animationStyle: AnimationStyle = AnimationStyle.NATURAL,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val gestures: GestureBindings = GestureBindings.Default,
    val libraryMode: LibraryMode = LibraryMode.DEMO,
    /** True once the user answered the first-run "use your photos" prompt. */
    val libraryChoiceMade: Boolean = false,
    /** Opt-in on-device model for coarse photo labels; the model is downloaded once. */
    val aiRecognitionEnabled: Boolean = false,
    /** Last filters used to start a session, newest first. */
    val recentFilters: List<PhotoFilter> = emptyList(),
)
