package app.photidy.core.model

/**
 * Pure state transition logic for a tidy session. Keeping it side-effect free makes it
 * easy to test and keeps persistence concerns inside the data layer.
 */
object SessionEngine {

    const val MAX_UNDO = 60

    fun startSession(
        snapshot: LibrarySnapshot,
        queue: List<String>,
        filter: PhotoFilter,
        now: Long,
    ): LibrarySnapshot {
        if (queue.isEmpty()) return snapshot
        val session = OrganizeSession(
            id = "session-$now",
            queue = queue,
            cursor = 0,
            startedAt = now,
            filter = filter,
        )
        return snapshot.copy(
            session = session,
            undoStack = emptyList(),
        )
    }

    fun commit(
        snapshot: LibrarySnapshot,
        action: GestureAction,
        albumId: String? = null,
    ): LibrarySnapshot {
        val session = snapshot.session ?: return snapshot
        if (session.isComplete) return snapshot
        if (action == GestureAction.NONE) return snapshot
        if (action.requiresAlbum && albumId == null) return snapshot

        val photoId = session.currentPhotoId ?: return snapshot
        val previous = snapshot.stateOf(photoId)
        val next = resolveState(previous, action, albumId)

        val updatedStates = if (next == previous) {
            snapshot.states
        } else {
            snapshot.states + (photoId to next)
        }

        val undoEntry = UndoEntry(
            photoId = photoId,
            previousState = previous,
            cursorBefore = session.cursor,
        )

        return snapshot.copy(
            states = updatedStates,
            session = session.copy(cursor = session.cursor + 1),
            undoStack = (snapshot.undoStack + undoEntry).takeLast(MAX_UNDO),
        )
    }

    fun undo(snapshot: LibrarySnapshot): LibrarySnapshot {
        val entry = snapshot.undoStack.lastOrNull() ?: return snapshot
        val session = snapshot.session ?: return snapshot

        val restoredStates = if (entry.previousState == PhotoState.Initial) {
            snapshot.states - entry.photoId
        } else {
            snapshot.states + (entry.photoId to entry.previousState)
        }

        return snapshot.copy(
            states = restoredStates,
            session = session.copy(cursor = entry.cursorBefore),
            undoStack = snapshot.undoStack.dropLast(1),
        )
    }

    fun resolveState(
        current: PhotoState,
        action: GestureAction,
        albumId: String?,
    ): PhotoState = when (action) {
        GestureAction.ADD_TO_DELETE_LIST -> current.copy(decision = PhotoDecision.DELETE_CANDIDATE)
        GestureAction.KEEP -> current.copy(decision = PhotoDecision.KEPT)
        GestureAction.FAVORITE -> current.copy(decision = PhotoDecision.KEPT, favorite = true)
        GestureAction.ADD_TO_ALBUM -> current.copy(
            decision = PhotoDecision.KEPT,
            albumOverrideId = albumId ?: current.albumOverrideId,
        )
        GestureAction.SKIP, GestureAction.NONE -> current
    }

    /**
     * Drops photos that no longer exist on the device. The queue keeps its remaining order and
     * the cursor is shifted by the number of removed photos before it, so the current card stays
     * stable after a real deletion.
     */
    fun forgetPhotos(snapshot: LibrarySnapshot, ids: Set<String>): LibrarySnapshot {
        if (ids.isEmpty()) return snapshot
        val session = snapshot.session
        val updatedSession = session?.let { current ->
            val removedBeforeCursor = current.queue.take(current.cursor).count { it in ids }
            val queue = current.queue.filterNot { it in ids }
            current.copy(
                queue = queue,
                cursor = (current.cursor - removedBeforeCursor).coerceIn(0, queue.size),
            )
        }
        return snapshot.copy(
            states = snapshot.states - ids,
            session = updatedSession,
            undoStack = snapshot.undoStack.filterNot { it.photoId in ids },
        )
    }
}
