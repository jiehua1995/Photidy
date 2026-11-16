package app.photidy.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
}

/**
 * The action a swipe (or a bottom-bar button) performs on the current photo.
 * [requiresAlbum] actions need a target album chosen by the user before committing.
 */
@Serializable
enum class GestureAction(val requiresAlbum: Boolean = false) {
    ADD_TO_DELETE_LIST,
    KEEP,
    FAVORITE,
    ADD_TO_ALBUM(requiresAlbum = true),
    SKIP,
    NONE,
}

@Serializable
data class GestureBindings(
    val left: GestureAction = GestureAction.ADD_TO_DELETE_LIST,
    val right: GestureAction = GestureAction.KEEP,
    val up: GestureAction = GestureAction.FAVORITE,
    val down: GestureAction = GestureAction.ADD_TO_ALBUM,
) {
    fun actionFor(direction: SwipeDirection): GestureAction = when (direction) {
        SwipeDirection.LEFT -> left
        SwipeDirection.RIGHT -> right
        SwipeDirection.UP -> up
        SwipeDirection.DOWN -> down
    }

    fun with(direction: SwipeDirection, action: GestureAction): GestureBindings = when (direction) {
        SwipeDirection.LEFT -> copy(left = action)
        SwipeDirection.RIGHT -> copy(right = action)
        SwipeDirection.UP -> copy(up = action)
        SwipeDirection.DOWN -> copy(down = action)
    }

    companion object {
        val Default = GestureBindings()
    }
}

@Serializable
data class OrganizeSession(
    val id: String,
    val queue: List<String>,
    val cursor: Int = 0,
    val startedAt: Long,
    val filter: PhotoFilter = PhotoFilter(),
) {
    val total: Int
        get() = queue.size

    val remaining: Int
        get() = (queue.size - cursor).coerceAtLeast(0)

    val isComplete: Boolean
        get() = cursor >= queue.size

    val currentPhotoId: String?
        get() = queue.getOrNull(cursor)
}

@Serializable
data class UndoEntry(
    val photoId: String,
    val previousState: PhotoState,
    val cursorBefore: Int,
)

@Serializable
data class LibrarySnapshot(
    val version: Int = 1,
    val states: Map<String, PhotoState> = emptyMap(),
    val session: OrganizeSession? = null,
    val undoStack: List<UndoEntry> = emptyList(),
) {
    fun stateOf(photoId: String): PhotoState = states[photoId] ?: PhotoState.Initial

    fun toPhotoWithStates(photos: List<PhotoItem>): List<PhotoWithState> =
        photos.map { PhotoWithState(it, stateOf(it.id)) }
}
