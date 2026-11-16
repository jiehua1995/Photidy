package app.photidy.feature.photodetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.PhotoDecision
import app.photidy.core.model.PhotoWithState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhotoDetailUiState(
    val isLoading: Boolean = true,
    val item: PhotoWithState? = null,
)

class PhotoDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val photoId: String = savedStateHandle.get<String>("photoId").orEmpty()
    private val photoRepository = PhotidyGraph.photoRepository

    val uiState: StateFlow<PhotoDetailUiState> = photoRepository.photos
        .map { photos ->
            PhotoDetailUiState(
                isLoading = false,
                item = photos.firstOrNull { it.photo.id == photoId },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PhotoDetailUiState(),
        )

    fun toggleFavorite() {
        viewModelScope.launch { photoRepository.toggleFavorite(photoId) }
    }

    fun markForDeletion() {
        viewModelScope.launch { photoRepository.markForDeletion(photoId) }
    }

    fun markAsKept() {
        viewModelScope.launch { photoRepository.markAsKept(photoId) }
    }

    fun removeFromDeleteList() {
        val decision = uiState.value.item?.state?.decision
        if (decision == PhotoDecision.DELETE_CANDIDATE) {
            viewModelScope.launch {
                photoRepository.removeFromDeleteList(setOf(photoId))
            }
        }
    }
}
