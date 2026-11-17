package app.photidy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.LibraryState
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.LibraryStatsCalculator
import app.photidy.core.model.LibraryMode
import app.photidy.core.model.PhotoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalPhotos: Int = 0,
    val organizedCount: Int = 0,
    val favoriteCount: Int = 0,
    val deleteCount: Int = 0,
    val deleteBytes: Long = 0,
    val activeSessionRemaining: Int? = null,
    val sessionCompleted: Boolean = false,
    val previewPhotos: List<PhotoItem> = emptyList(),
    val library: LibraryState = LibraryState(),
    val showLibraryPrompt: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val settingsRepository = PhotidyGraph.settingsRepository

    val uiState: StateFlow<HomeUiState> = combine(
        photoRepository.photos,
        photoRepository.snapshot,
        settingsRepository.settings,
        photoRepository.libraryState,
    ) { photos, snapshot, settings, library ->
        val stats = LibraryStatsCalculator.calculate(photos)
        val session = snapshot.session
        val active = session?.takeUnless { it.isComplete }

        val previews = if (active != null) {
            val byId = photos.associateBy { it.photo.id }
            active.queue.drop(active.cursor).take(3).mapNotNull { byId[it]?.photo }
        } else {
            photos.sortedByDescending { it.photo.capturedAt }.take(3).map { it.photo }
        }

        HomeUiState(
            isLoading = false,
            totalPhotos = stats.total,
            organizedCount = stats.organized,
            favoriteCount = stats.favorites,
            deleteCount = stats.deleteCount,
            deleteBytes = stats.deleteBytes,
            activeSessionRemaining = active?.remaining,
            sessionCompleted = session?.isComplete == true,
            previewPhotos = previews,
            library = library,
            showLibraryPrompt = !settings.libraryChoiceMade,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Called after the read permission request succeeds. */
    fun useDeviceLibrary() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(libraryMode = LibraryMode.DEVICE, libraryChoiceMade = true)
            }
            photoRepository.applyMode(LibraryMode.DEVICE)
            photoRepository.refreshDeviceLibrary()
        }
    }

    /** Keeps the bundled demo library and stops asking on the home screen. */
    fun keepDemoLibrary() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(libraryMode = LibraryMode.DEMO, libraryChoiceMade = true)
            }
            photoRepository.applyMode(LibraryMode.DEMO)
        }
    }
}
