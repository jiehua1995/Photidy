package app.photidy.feature.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.AlbumMove
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.Album
import app.photidy.core.model.GestureAction
import app.photidy.core.model.GestureBindings
import app.photidy.core.model.PhotoDecision
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.SwipeDirection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionStatsUi(
    val reviewed: Int = 0,
    val total: Int = 0,
    val markedForDeletion: Int = 0,
    val favorites: Int = 0,
)

data class OrganizerUiState(
    val isLoading: Boolean = true,
    val hasSession: Boolean = false,
    val isComplete: Boolean = false,
    val current: PhotoWithState? = null,
    val next: PhotoWithState? = null,
    val upNext: List<PhotoWithState> = emptyList(),
    val total: Int = 0,
    val cursor: Int = 0,
    val remaining: Int = 0,
    val canUndo: Boolean = false,
    val gestureBindings: GestureBindings = GestureBindings.Default,
    val hapticsEnabled: Boolean = true,
    val pendingAlbumPhotoId: String? = null,
    val albums: List<Album> = emptyList(),
    val stats: SessionStatsUi? = null,
) {
    val progress: Float
        get() = if (total == 0) 0f else cursor.toFloat() / total
}

sealed interface OrganizerEvent {
    data object Undone : OrganizerEvent

    /** Asks the route to show the system write confirmation for a real album move. */
    data class RequestMove(val move: AlbumMove) : OrganizerEvent

    data class MoveDone(val albumId: String, val albumTitle: String?) : OrganizerEvent

    data object MoveFailed : OrganizerEvent
}

class OrganizerViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository
    private val settingsRepository = PhotidyGraph.settingsRepository
    private val pendingAlbum = MutableStateFlow<String?>(null)
    private val eventChannel = Channel<OrganizerEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val uiState: StateFlow<OrganizerUiState> = combine(
        photoRepository.photos,
        photoRepository.snapshot,
        settingsRepository.settings,
        pendingAlbum,
        photoRepository.albums,
    ) { photos, snapshot, settings, pendingAlbumPhotoId, albums ->
        val byId = photos.associateBy { it.photo.id }
        val session = snapshot.session
        val queuePhotos = session?.queue.orEmpty().mapNotNull { byId[it] }
        val current = session?.currentPhotoId?.let { byId[it] }
        val next = session?.queue?.getOrNull(session.cursor + 1)?.let { byId[it] }
        val upNext = session?.queue
            ?.drop(session.cursor + 2)
            .orEmpty()
            .take(5)
            .mapNotNull { byId[it] }

        val stats = session?.let {
            SessionStatsUi(
                reviewed = queuePhotos.count { photo -> photo.state.isReviewed },
                total = it.total,
                markedForDeletion = queuePhotos.count { photo ->
                    photo.state.decision == PhotoDecision.DELETE_CANDIDATE
                },
                favorites = queuePhotos.count { photo -> photo.state.favorite },
            )
        }

        OrganizerUiState(
            isLoading = false,
            hasSession = session != null,
            isComplete = session?.isComplete == true && session.total > 0,
            current = current,
            next = next,
            upNext = upNext,
            total = session?.total ?: 0,
            cursor = session?.cursor ?: 0,
            remaining = session?.remaining ?: 0,
            canUndo = snapshot.undoStack.isNotEmpty(),
            gestureBindings = settings.gestures,
            hapticsEnabled = settings.hapticsEnabled,
            pendingAlbumPhotoId = pendingAlbumPhotoId,
            albums = albums,
            stats = stats,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrganizerUiState(albums = photoRepository.albums.value),
    )

    fun onDirection(direction: SwipeDirection) {
        performAction(uiState.value.gestureBindings.actionFor(direction))
    }

    fun onAction(action: GestureAction) {
        performAction(action)
    }

    private fun performAction(action: GestureAction) {
        if (action == GestureAction.NONE) return
        if (action.requiresAlbum) {
            pendingAlbum.value = uiState.value.current?.photo?.id
            return
        }
        viewModelScope.launch {
            photoRepository.commitAction(action)
        }
    }

    fun chooseAlbum(albumId: String) {
        val photoId = pendingAlbum.value ?: return
        pendingAlbum.value = null
        val move = photoRepository.albumMoveRequest(photoId, albumId)
        if (move == null) {
            // Demo photos, albums without a MediaStore path or Android 10 keep the plan-only flow.
            viewModelScope.launch {
                photoRepository.commitAction(GestureAction.ADD_TO_ALBUM, albumId)
            }
            return
        }
        viewModelScope.launch {
            eventChannel.send(OrganizerEvent.RequestMove(move))
        }
    }

    /** Called by the route with the result of the system write confirmation. */
    fun onMoveResult(move: AlbumMove, granted: Boolean) {
        if (!granted) {
            viewModelScope.launch { eventChannel.send(OrganizerEvent.MoveFailed) }
            return
        }
        viewModelScope.launch {
            val moved = photoRepository.completeAlbumMove(move)
            if (moved) {
                val album = photoRepository.albums.value.firstOrNull { it.id == move.albumId }
                eventChannel.send(OrganizerEvent.MoveDone(move.albumId, album?.title))
            } else {
                eventChannel.send(OrganizerEvent.MoveFailed)
            }
        }
    }

    fun dismissAlbumPicker() {
        pendingAlbum.value = null
    }

    fun undo() {
        viewModelScope.launch {
            if (photoRepository.undo()) {
                eventChannel.send(OrganizerEvent.Undone)
            }
        }
    }
}
