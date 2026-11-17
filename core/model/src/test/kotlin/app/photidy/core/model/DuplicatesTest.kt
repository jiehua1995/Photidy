package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatesTest {

    private val now = 1_700_000_000_000L

    private fun photo(
        id: String,
        capturedAt: Long = now,
        sizeBytes: Long = 2_000_000,
        width: Int = 4000,
        height: Int = 3000,
        isVideo: Boolean = false,
    ) = PhotoItem(
        id = id,
        name = "IMG_$id.jpg",
        albumId = "camera",
        capturedAt = capturedAt,
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        isVideo = isVideo,
    )

    @Test
    fun `pending groups skip photos that are already reviewed`() {
        val items = listOf(
            PhotoWithState(photo("a"), PhotoState(decision = PhotoDecision.KEPT)),
            PhotoWithState(photo("b"), PhotoState(decision = PhotoDecision.DELETE_CANDIDATE)),
            PhotoWithState(photo("c")),
        )

        val groups = DuplicateFinder.pendingGroups(items)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `reviewed member drops out and singleton groups disappear`() {
        val items = listOf(
            PhotoWithState(photo("a"), PhotoState(decision = PhotoDecision.DELETE_CANDIDATE)),
            PhotoWithState(photo("b")),
            PhotoWithState(photo("c")),
            PhotoWithState(photo("d")),
        )

        val groups = DuplicateFinder.pendingGroups(items)

        assertEquals(1, groups.size)
        assertEquals(setOf("b", "c", "d"), groups.single().photos.mapTo(HashSet()) { it.id })
    }

    @Test
    fun `newest capture is suggested when nothing is favourited`() {
        val items = listOf(
            PhotoWithState(photo("old", capturedAt = now - 5_000)),
            PhotoWithState(photo("new", capturedAt = now)),
            PhotoWithState(photo("middle", capturedAt = now - 1_000)),
        )

        val group = DuplicateFinder.pendingGroups(items).single()

        assertEquals("new", group.suggestedKeepId)
    }

    @Test
    fun `favourites win over newer photos`() {
        val items = listOf(
            PhotoWithState(photo("old", capturedAt = now - 5_000), PhotoState(favorite = true)),
            PhotoWithState(photo("new", capturedAt = now)),
        )

        val group = DuplicateFinder.pendingGroups(items).single()

        assertEquals("old", group.suggestedKeepId)
    }

    @Test
    fun `reclaimable bytes exclude the suggested keeper`() {
        val items = listOf(
            PhotoWithState(photo("a", sizeBytes = 2_000_000)),
            PhotoWithState(photo("b", sizeBytes = 2_000_000)),
            PhotoWithState(photo("c", sizeBytes = 2_000_000)),
        )

        val group = DuplicateFinder.pendingGroups(items).single()

        assertEquals(4_000_000L, group.reclaimableBytes)
    }

    @Test
    fun `videos and singletons are ignored`() {
        val items = listOf(
            PhotoWithState(photo("v1", isVideo = true)),
            PhotoWithState(photo("v2", isVideo = true)),
            PhotoWithState(photo("solo", sizeBytes = 9_000_000)),
        )

        assertTrue(DuplicateFinder.allGroups(items).isEmpty())
    }
}
