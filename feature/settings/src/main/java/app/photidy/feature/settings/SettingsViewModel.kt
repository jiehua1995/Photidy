package app.photidy.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.AiModelState
import app.photidy.core.data.LibraryState
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.localization.LocaleController
import app.photidy.core.model.AnimationStyle
import app.photidy.core.model.AppLanguage
import app.photidy.core.model.AppSettings
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.LibraryMode
import app.photidy.core.model.SwipeDirection
import app.photidy.core.model.ThemeMode
import app.photidy.core.model.VisualStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val settingsRepository = PhotidyGraph.settingsRepository
    private val photoRepository = PhotidyGraph.photoRepository

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val library: StateFlow<LibraryState> = photoRepository.libraryState

    val aiModel: StateFlow<AiModelState> = PhotidyGraph.aiModelManager.state

    /** Turning the feature on fetches the small model once; turning it off stops a download. */
    fun setAiRecognition(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(aiRecognitionEnabled = enabled) }
            if (enabled) {
                if (!PhotidyGraph.aiModelManager.isReady()) {
                    PhotidyGraph.aiModelManager.download()
                }
            } else {
                PhotidyGraph.aiModelManager.cancelDownload()
            }
        }
    }

    fun downloadAiModel() {
        PhotidyGraph.aiModelManager.download()
    }

    fun deleteAiModel() {
        PhotidyGraph.aiModelManager.deleteModel()
    }

    fun setThemeMode(mode: ThemeMode) {
        update { it.copy(themeMode = mode) }
    }

    fun setVisualStyle(style: VisualStyle) {
        update { it.copy(visualStyle = style) }
    }

    fun setEffectIntensity(value: Float) {
        update { it.copy(effectIntensity = value.coerceIn(0f, 1f)) }
    }

    fun setAnimationStyle(style: AnimationStyle) {
        update { it.copy(animationStyle = style) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        update { it.copy(animationsEnabled = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        update { it.copy(hapticsEnabled = enabled) }
    }

    fun setGesture(direction: SwipeDirection, action: GestureAction) {
        update { settings ->
            settings.copy(gestures = settings.gestures.with(direction, action))
        }
    }

    fun resetGestures() {
        update { it.copy(gestures = GestureBindings.Default) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(language = language) }
            LocaleController.apply(language)
        }
    }

    fun selectLibraryMode(mode: LibraryMode) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(libraryMode = mode, libraryChoiceMade = true)
            }
            photoRepository.applyMode(mode)
        }
    }

    fun refreshLibrary() {
        photoRepository.refreshDeviceLibrary()
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }
}
