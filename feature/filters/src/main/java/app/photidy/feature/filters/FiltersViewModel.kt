package app.photidy.feature.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.AiModelState
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.AiTag
import app.photidy.core.model.Album
import app.photidy.core.model.DateRangeFilter
import app.photidy.core.model.MediaFilter
import app.photidy.core.model.PhotoFilter
import app.photidy.core.model.PhotoSort
import app.photidy.core.model.SmartFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val aiModel: AiModelState = AiModelState.NotDownloaded,
    val aiProgress: AiProgress = AiProgress(),
    val hasDevicePhotos: Boolean = false,
)

private data class AiInfo(
    val enabled: Boolean,
    val model: AiModelState,
)

class FiltersViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val settingsRepository = PhotidyGraph.settingsRepository
    private val aiModelManager = PhotidyGraph.aiModelManager
    private val aiTagger = PhotidyGraph.aiTagger

    /** Reviewed photos stay out of the way by default, so a second pass resumes where it ended. */
    private val defaultFilter = PhotoFilter(unorganizedOnly = true)
    private val filter = MutableStateFlow(defaultFilter)
    private val aiProgress = MutableStateFlow(AiProgress())
    private var aiJob: Job? = null

    private val aiInfo = combine(
        settingsRepository.settings,
        aiModelManager.state,
    ) { settings, model -> AiInfo(settings.aiRecognitionEnabled, model) }

    val uiState: StateFlow<FiltersUiState> = combine(
        filter,
        photoRepository.photos,
        photoRepository.albums,
        aiInfo,
        aiProgress,
    ) { current, photos, albums, ai, progress ->
        FiltersUiState(
            isLoading = false,
            filter = current,
            matchCount = current.applyTo(photos, System.currentTimeMillis()).size,
            albums = albums,
            aiEnabled = ai.enabled,
            aiModel = ai.model,
            aiProgress = progress,
            hasDevicePhotos = photos.any { it.photo.contentUri != null },
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

    fun reset() {
        aiJob?.cancel()
        aiProgress.value = AiProgress()
        filter.value = defaultFilter
    }

    suspend fun startSession(): Int = photoRepository.startSession(filter.value)

    private fun updateFilter(transform: (PhotoFilter) -> PhotoFilter) {
        filter.value = transform(filter.value)
        // Newly visible photos need labels before the AI filter can match them.
        if (filter.value.aiTag != null) {
            startAiAnalysis()
        }
    }

    /**
     * Labels the photos that are currently in scope and were not analyzed yet. Work is capped so
     * the first batch finishes quickly; the next filter change picks up the rest.
     */
    private fun startAiAnalysis() {
        if (!aiModelManager.isReady() || aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            val photos = photoRepository.photos.first()
            val candidates = filter.value
                .copy(aiTag = null)
                .applyTo(photos, System.currentTimeMillis())
                .filter { it.photo.contentUri != null && !it.state.aiAnalyzed }
                .take(MAX_AI_BATCH)
            if (candidates.isEmpty()) return@launch

            aiProgress.value = AiProgress(analyzing = true, done = 0, total = candidates.size)
            val analyzed = HashSet<String>()
            val tags = HashMap<String, List<AiTag>>()
            candidates.forEachIndexed { index, item ->
                val result = aiTagger.analyze(item.photo)
                analyzed += item.photo.id
                if (result.isNotEmpty()) tags[item.photo.id] = result
                aiProgress.value = AiProgress(
                    analyzing = true,
                    done = index + 1,
                    total = candidates.size,
                )
            }
            photoRepository.saveAiTags(analyzed, tags)
            aiProgress.value = AiProgress(
                analyzing = false,
                done = candidates.size,
                total = candidates.size,
            )
        }
    }

    private companion object {
        const val MAX_AI_BATCH = 80
    }
}
