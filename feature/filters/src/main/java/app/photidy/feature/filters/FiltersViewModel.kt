package app.photidy.feature.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.AiModelState
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.AiModelTier
import app.photidy.core.model.AiTag
import app.photidy.core.model.Album
import app.photidy.core.model.DateRangeFilter
import app.photidy.core.model.MediaFilter
import app.photidy.core.model.PhotoFilter
import app.photidy.core.model.PhotoSort
import app.photidy.core.model.SmartFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AiProgress(
    val analyzing: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
)

data class FiltersUiState(
    val isLoading: Boolean = true,
    val filter: PhotoFilter = PhotoFilter(),
    val matchCount: Int = 0,
    val albums: List<Album> = emptyList(),
    val aiEnabled: Boolean = false,
    val aiModel: AiModelState = AiModelState.NotDownloaded(AiModelTier.LIGHT),
    val aiProgress: AiProgress = AiProgress(),
    val hasDevicePhotos: Boolean = false,
    val recentFilters: List<PhotoFilter> = emptyList(),
)

private data class FiltersSettings(
    val aiEnabled: Boolean,
    val model: AiModelState,
    val recentFilters: List<PhotoFilter>,
)

class FiltersViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val settingsRepository = PhotidyGraph.settingsRepository
    private val aiModelManager = PhotidyGraph.aiModelManager

    /** Reviewed photos stay out of the way by default, so a second pass resumes where it ended. */
    private val defaultFilter = PhotoFilter(unorganizedOnly = true)
    private val filter = MutableStateFlow(defaultFilter)
    private val aiProgress = PhotidyGraph.aiAnalysis.progress.map { scan ->
        AiProgress(analyzing = scan.running, done = scan.done, total = scan.total)
    }

    private val aiInfo = combine(
        settingsRepository.settings,
        aiModelManager.state,
    ) { settings, model ->
        FiltersSettings(
            aiEnabled = settings.aiRecognitionEnabled,
            model = model,
            recentFilters = settings.recentFilters,
        )
    }

    val uiState: StateFlow<FiltersUiState> = combine(
        filter,
        photoRepository.photos,
        photoRepository.albums,
        aiInfo,
        aiProgress,
    ) { current, photos, albums, info, progress ->
        FiltersUiState(
            isLoading = false,
            filter = current,
            matchCount = current.applyTo(photos, System.currentTimeMillis()).size,
            albums = albums,
            aiEnabled = info.aiEnabled,
            aiModel = info.model,
            aiProgress = progress,
            hasDevicePhotos = photos.any { it.photo.contentUri != null },
            recentFilters = info.recentFilters,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FiltersUiState(
            filter = defaultFilter,
            albums = photoRepository.albums.value,
        ),
    )

    fun selectAlbum(albumId: String?) {
        updateFilter { current ->
            if (albumId == null) {
                // Smart picks and AI tags are scoped to a single album.
                current.copy(albumId = null, smart = SmartFilter.NONE, aiTag = null)
            } else {
                current.copy(albumId = albumId)
            }
        }
    }

    fun selectMedia(media: MediaFilter) {
        updateFilter { it.copy(media = media) }
    }

    fun selectDateRange(range: DateRangeFilter) {
        updateFilter { it.copy(dateRange = range) }
    }

    fun selectSort(sort: PhotoSort) {
        updateFilter { it.copy(sort = sort) }
    }

    fun selectSmart(smart: SmartFilter) {
        updateFilter { it.copy(smart = smart) }
    }

    fun selectAiTag(tag: AiTag?) {
        updateFilter { it.copy(aiTag = tag) }
    }

    fun setUnorganizedOnly(value: Boolean) {
        updateFilter { it.copy(unorganizedOnly = value) }
    }

    fun applyRecent(recent: PhotoFilter) {
        filter.value = recent
    }

    fun reset() {
        filter.value = defaultFilter
    }

    /** Starts a session and remembers the filter so it can be reused from the top of the screen. */
    suspend fun startSession(): Int {
        val current = filter.value
        val count = photoRepository.startSession(current)
        if (count > 0) {
            settingsRepository.update { settings ->
                settings.copy(
                    recentFilters = (listOf(current) + settings.recentFilters.filterNot { it == current })
                        .take(MAX_RECENT_FILTERS),
                )
            }
        }
        return count
    }

    private fun updateFilter(transform: (PhotoFilter) -> PhotoFilter) {
        filter.value = transform(filter.value)
    }

    private companion object {
        const val MAX_RECENT_FILTERS = 3
    }
}
