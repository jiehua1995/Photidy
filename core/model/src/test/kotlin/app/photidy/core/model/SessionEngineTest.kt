package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    private val now = 1_760_000_000_000L
    private val queue = listOf("photo_1", "photo_2", "photo_3")

    private fun sessionSnapshot(): LibrarySnapshot =
        SessionEngine.startSession(LibrarySnapshot(), queue, PhotoFilter(), now)

    @Test
    fun `starting a session stores the queue and resets undo`() {
        val snapshot = sessionSnapshot()

        assertEquals(queue, snapshot.session?.queue)
        assertEquals(0, snapshot.session?.cursor)
        assertTrue(snapshot.undoStack.isEmpty())
        assertFalse(snapshot.session?.isComplete == true)
    }

    @Test
    fun `marking for deletion advances the cursor`() {
        val result = SessionEngine.commit(sessionSnapshot(), GestureAction.ADD_TO_DELETE_LIST)

        assertEquals(1, result.session?.cursor)
        assertEquals(PhotoDecision.DELETE_CANDIDATE, result.stateOf("photo_1").decision)
        assertEquals(1, result.undoStack.size)
    }

    @Test
    fun `keeping a photo overwrites a previous deletion decision`() {
        val first = SessionEngine.commit(sessionSnapshot(), GestureAction.ADD_TO_DELETE_LIST)
        val undo = SessionEngine.undo(first)
        val kept = SessionEngine.commit(undo, GestureAction.KEEP)

        assertEquals(PhotoDecision.KEPT, kept.stateOf("photo_1").decision)
        assertFalse(kept.stateOf("photo_1").favorite)
    }

    @Test
    fun `favoriting also marks the photo as kept`() {
        val result = SessionEngine.commit(sessionSnapshot(), GestureAction.FAVORITE)
        val state = result.stateOf("photo_1")

        assertTrue(state.favorite)
        assertEquals(PhotoDecision.KEPT, state.decision)
    }

    @Test
    fun `adding to an album requires a target`() {
        val withoutAlbum = SessionEngine.commit(sessionSnapshot(), GestureAction.ADD_TO_ALBUM)
        assertEquals(0, withoutAlbum.session?.cursor)

        val withAlbum = SessionEngine.commit(
            sessionSnapshot(),
            GestureAction.ADD_TO_ALBUM,
            albumId = "travel",
        )
        assertEquals(1, withAlbum.session?.cursor)
        assertEquals("travel", withAlbum.stateOf("photo_1").albumOverrideId)
        assertEquals(PhotoDecision.KEPT, withAlbum.stateOf("photo_1").decision)
    }

    @Test
    fun `skipping advances without changing the photo state`() {
        val result = SessionEngine.commit(sessionSnapshot(), GestureAction.SKIP)

        assertEquals(1, result.session?.cursor)
        assertFalse(result.states.containsKey("photo_1"))
        assertEquals(1, result.undoStack.size)
    }

    @Test
    fun `a no-op action leaves the snapshot untouched`() {
        val initial = sessionSnapshot()
        val result = SessionEngine.commit(initial, GestureAction.NONE)

        assertEquals(initial, result)
    }

    @Test
    fun `undo restores the previous decision and cursor`() {
        val first = SessionEngine.commit(sessionSnapshot(), GestureAction.ADD_TO_DELETE_LIST)
        val second = SessionEngine.commit(first, GestureAction.KEEP)
        val undone = SessionEngine.undo(second)

        assertEquals(1, undone.session?.cursor)
        assertEquals(PhotoDecision.DELETE_CANDIDATE, undone.stateOf("photo_1").decision)
        assertFalse(undone.states.containsKey("photo_2"))
    }

    @Test
    fun `undo also reverts a skip`() {
        val skipped = SessionEngine.commit(sessionSnapshot(), GestureAction.SKIP)
        val undone = SessionEngine.undo(skipped)

        assertEquals(0, undone.session?.cursor)
        assertTrue(undone.states.isEmpty())
    }

    @Test
    fun `undo with an empty stack is safe`() {
        val snapshot = sessionSnapshot()
        assertEquals(snapshot, SessionEngine.undo(snapshot))
    }

    @Test
    fun `session completes after the whole queue is reviewed`() {
        var snapshot = sessionSnapshot()
        repeat(queue.size) {
            snapshot = SessionEngine.commit(snapshot, GestureAction.KEEP)
        }

        assertTrue(snapshot.session?.isComplete == true)
        assertEquals(0, snapshot.session?.remaining)
        assertEquals(PhotoDecision.KEPT, snapshot.stateOf("photo_3").decision)

        val extra = SessionEngine.commit(snapshot, GestureAction.KEEP)
        assertEquals(snapshot, extra)
    }

    @Test
    fun `forgetting a photo before the cursor keeps the current photo in place`() {
        var snapshot = sessionSnapshot()
        snapshot = SessionEngine.commit(snapshot, GestureAction.KEEP)
        snapshot = SessionEngine.commit(snapshot, GestureAction.KEEP)

        val result = SessionEngine.forgetPhotos(snapshot, setOf("photo_1"))

        assertEquals(listOf("photo_2", "photo_3"), result.session?.queue)
        assertEquals(1, result.session?.cursor)
        assertEquals("photo_3", result.session?.currentPhotoId)
        assertFalse(result.states.containsKey("photo_1"))
    }

    @Test
    fun `forgetting the current photo keeps the next photo selected`() {
        val snapshot = SessionEngine.commit(sessionSnapshot(), GestureAction.ADD_TO_DELETE_LIST)

        val result = SessionEngine.forgetPhotos(snapshot, setOf("photo_1"))

        assertEquals(listOf("photo_2", "photo_3"), result.session?.queue)
        assertEquals(0, result.session?.cursor)
        assertEquals("photo_2", result.session?.currentPhotoId)
    }

    @Test
    fun `forgetting photos also drops their undo entries`() {
        var snapshot = sessionSnapshot()
        snapshot = SessionEngine.commit(snapshot, GestureAction.KEEP)
        snapshot = SessionEngine.commit(snapshot, GestureAction.KEEP)

        val result = SessionEngine.forgetPhotos(snapshot, setOf("photo_2"))

        assertEquals(1, result.undoStack.size)
        assertEquals("photo_1", result.undoStack.first().photoId)
        assertFalse(result.states.containsKey("photo_2"))
    }

    @Test
    fun `forgetting everything completes the session without a crash`() {
        val snapshot = sessionSnapshot()

        val result = SessionEngine.forgetPhotos(snapshot, queue.toSet())

        assertTrue(result.session?.queue.isNullOrEmpty())
        assertEquals(0, result.session?.cursor)
        assertTrue(result.session?.isComplete == true)
    }

    @Test
    fun `batch keep marks every selected photo and skips them`() {
        val result = SessionEngine.commitBatch(
            sessionSnapshot(),
            setOf("photo_1", "photo_2"),
            GestureAction.KEEP,
        )

        assertEquals(PhotoDecision.KEPT, result.stateOf("photo_1").decision)
        assertEquals(PhotoDecision.KEPT, result.stateOf("photo_2").decision)
        assertEquals(PhotoDecision.UNREVIEWED, result.stateOf("photo_3").decision)
        assertEquals(2, result.session?.cursor)
        assertEquals("photo_3", result.session?.currentPhotoId)
    }

    @Test
    fun `batch delete marks only queued photos`() {
        val result = SessionEngine.commitBatch(
            sessionSnapshot(),
            setOf("photo_1", "not_in_queue"),
            GestureAction.ADD_TO_DELETE_LIST,
        )

        assertEquals(PhotoDecision.DELETE_CANDIDATE, result.stateOf("photo_1").decision)
        assertFalse(result.states.containsKey("not_in_queue"))
        assertEquals(1, result.session?.cursor)
    }

    @Test
    fun `batch with an empty selection changes nothing`() {
        val snapshot = sessionSnapshot()
        assertEquals(snapshot, SessionEngine.commitBatch(snapshot, emptySet(), GestureAction.KEEP))
    }

    @Test
    fun `batch undo restores one photo at a time`() {
        val result = SessionEngine.commitBatch(
            sessionSnapshot(),
            setOf("photo_1", "photo_2"),
            GestureAction.KEEP,
        )
        val undone = SessionEngine.undo(result)

        assertEquals(0, undone.session?.cursor)
        assertEquals(1, undone.undoStack.size)
        assertEquals(PhotoDecision.KEPT, undone.stateOf("photo_1").decision)
        assertFalse(undone.states.containsKey("photo_2"))
    }
}
