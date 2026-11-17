package app.photidy.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.PhotoFilter
import app.photidy.core.model.PhotoSort
import app.photidy.core.model.PhotoWithState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val items: List<PhotoWithState> = emptyList(),
    val totalBytes: Long = 0L,
)

class FavoritesViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository

    val uiState: StateFlow<FavoritesUiState> = photoRepository.photos
        .map { photos ->
            val items = photos
                .filter { it.state.favorite }
                .sortedByDescending { it.photo.capturedAt }
            FavoritesUiState(
                isLoading = false,
                items = items,
                totalBytes = items.sumOf { it.photo.sizeBytes },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState(),
        )

    /** Starts a tidy session limited to favorites and returns the queue size. */
    suspend fun startSession(): Int = photoRepository.startSession(
        PhotoFilter(favoritesOnly = true, sort = PhotoSort.NEWEST_FIRST),
    )
}
