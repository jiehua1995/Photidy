package app.photidy.feature.review

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.MediaRequest
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.PhotoDecision
import app.photidy.core.model.PhotoWithState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val isLoading: Boolean = true,
    val items: List<PhotoWithState> = emptyList(),
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val totalBytes: Long = 0L,
    /** Real files among the current selection; only these can be deleted from the device. */
    val deletableCount: Int = 0,
    val trashCount: Int = 0,
    val trashExpiryMillis: Long = 0L,
)

sealed interface ReviewEvent {
    /** Launches the system confirmation sheet; the result comes back through [onDeleteResult]. */
    data class RequestDelete(val intentSender: IntentSender) : ReviewEvent

    data object DeleteUnsupported : ReviewEvent

    data object DeleteFailed : ReviewEvent

    data class Deleted(val count: Int) : ReviewEvent
}

private data class Selection(
    val active: Boolean = false,
    val ids: Set<String> = emptySet(),
)

class ReviewViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val selection = MutableStateFlow(Selection())
    private val eventChannel = Channel<ReviewEvent>(Channel.BUFFERED)
    private var pendingDeleteIds: Set<String> = emptySet()

    val events = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ReviewUiState> = combine(
        photoRepository.photos,
        selection,
        photoRepository.trashed,
    ) { photos, currentSelection, trash ->
        val items = photos
            .filter { it.state.decision == PhotoDecision.DELETE_CANDIDATE }
            .sortedByDescending { it.photo.capturedAt }
        val existingIds = items.mapTo(HashSet()) { it.photo.id }
        val selected = currentSelection.ids.intersect(existingIds)
        ReviewUiState(
            isLoading = false,
            items = items,
            selectionMode = currentSelection.active,
            selectedIds = selected,
            totalBytes = items.sumOf { it.photo.sizeBytes },
            deletableCount = items.count { it.photo.id in selected && it.photo.contentUri != null },
            trashCount = trash.count,
            trashExpiryMillis = trash.earliestExpiryMillis,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReviewUiState(),
    )

    fun toggleSelectionMode() {
        val current = selection.value
        selection.value = if (current.active) {
            Selection()
        } else {
            Selection(active = true)
        }
    }

    fun enterSelection(photoId: String) {
        selection.value = Selection(active = true, ids = setOf(photoId))
    }

    fun toggleSelection(photoId: String) {
        val current = selection.value
        val ids = if (photoId in current.ids) current.ids - photoId else current.ids + photoId
        selection.value = Selection(active = true, ids = ids)
    }

    fun selectAll() {
        selection.value = Selection(
            active = true,
            ids = uiState.value.items.mapTo(HashSet()) { it.photo.id },
        )
    }

    fun clearSelection() {
        selection.value = Selection(active = true)
    }

    fun removeSelected() {
        val ids = selection.value.ids
        if (ids.isEmpty()) return
        viewModelScope.launch {
            photoRepository.removeFromDeleteList(ids)
            selection.value = Selection()
        }
    }

    /** Asks Android to delete the selected real photos after the system confirmation. */
    fun deleteSelected() {
        val ids = selection.value.ids
        if (ids.isEmpty()) return
        when (val request = photoRepository.deleteRequest(ids)) {
            is MediaRequest.Confirmation -> {
                pendingDeleteIds = ids
                viewModelScope.launch {
                    eventChannel.send(ReviewEvent.RequestDelete(request.intentSender))
                }
            }
            MediaRequest.Unsupported -> viewModelScope.launch {
                eventChannel.send(ReviewEvent.DeleteUnsupported)
            }
        }
    }

    /** Called by the route with the result of the system delete sheet. */
    fun onDeleteResult(granted: Boolean) {
        val ids = pendingDeleteIds
        pendingDeleteIds = emptySet()
        if (!granted || ids.isEmpty()) {
            viewModelScope.launch { eventChannel.send(ReviewEvent.DeleteFailed) }
            return
        }
        viewModelScope.launch {
            photoRepository.completeDeletion(ids)
            selection.value = Selection()
            eventChannel.send(ReviewEvent.Deleted(ids.size))
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            photoRepository.clearDeleteList()
            selection.value = Selection()
        }
    }
}
