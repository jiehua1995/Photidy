package app.photidy.feature.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
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
import kotlinx.coroutines.flow.stateIn

data class FiltersUiState(
    val isLoading: Boolean = true,
    val filter: PhotoFilter = PhotoFilter(),
    val matchCount: Int = 0,
    val albums: List<Album> = emptyList(),
)

class FiltersViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository

    /** Reviewed photos stay out of the way by default, so a second pass resumes where it ended. */
    private val defaultFilter = PhotoFilter(unorganizedOnly = true)
    private val filter = MutableStateFlow(defaultFilter)

    val uiState: StateFlow<FiltersUiState> = combine(
        filter,
        photoRepository.photos,
        photoRepository.albums,
    ) { current, photos, albums ->
        FiltersUiState(
            isLoading = false,
            filter = current,
            matchCount = current.applyTo(photos, System.currentTimeMillis()).size,
            albums = albums,
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
        filter.value = if (albumId == null) {
            // Smart picks are scoped to a single album; dropping the album also drops them.
            filter.value.copy(albumId = null, smart = SmartFilter.NONE)
        } else {
            filter.value.copy(albumId = albumId)
        }
    }

    fun selectMedia(media: MediaFilter) {
        filter.value = filter.value.copy(media = media)
    }

    fun selectDateRange(range: DateRangeFilter) {
        filter.value = filter.value.copy(dateRange = range)
    }

    fun selectSort(sort: PhotoSort) {
        filter.value = filter.value.copy(sort = sort)
    }

    fun selectSmart(smart: SmartFilter) {
        filter.value = filter.value.copy(smart = smart)
    }

    fun setUnorganizedOnly(value: Boolean) {
        filter.value = filter.value.copy(unorganizedOnly = value)
    }

    fun reset() {
        filter.value = defaultFilter
    }

    suspend fun startSession(): Int = photoRepository.startSession(filter.value)
}
