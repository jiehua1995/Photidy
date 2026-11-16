package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsCalculatorTest {

    private val now = System.currentTimeMillis()

    private fun photo(
        id: String,
        size: Long = 2_000_000,
        capturedAt: Long = now - 3_600_000,
    ) = PhotoItem(
        id = id,
        name = "IMG_$id",
        albumId = "camera",
        capturedAt = capturedAt,
        width = 4000,
        height = 3000,
        sizeBytes = size,
        scene = PhotoScene.LAKE_REFLECTION,
        artSeed = 1,
    )

    @Test
    fun `counts reflect persisted state`() {
        val photos = listOf(
            PhotoWithState(photo("a"), PhotoState(decision = PhotoDecision.KEPT)),
            PhotoWithState(
                photo("b", size = 5_000_000),
                PhotoState(decision = PhotoDecision.DELETE_CANDIDATE),
            ),
            PhotoWithState(photo("c"), PhotoState(favorite = true, decision = PhotoDecision.KEPT)),
            PhotoWithState(photo("d")),
        )

        val summary = InsightsCalculator.calculate(photos, session = null, now = now)

        assertEquals(4, summary.totalPhotos)
        assertEquals(3, summary.organizedCount)
        assertEquals(1, summary.deleteCandidateCount)
        assertEquals(1, summary.favoriteCount)
        assertEquals(5_000_000L, summary.reclaimBytes)
        assertEquals(0.75f, summary.organizedRatio, 0.001f)
    }

    @Test
    fun `monthly distribution always has twelve buckets`() {
        val photos = listOf(
            PhotoWithState(photo("a", capturedAt = now)),
            PhotoWithState(photo("b", capturedAt = now - 30L * 24 * 60 * 60 * 1000)),
        )

        val summary = InsightsCalculator.calculate(photos, session = null, now = now)

        assertEquals(12, summary.monthly.size)
        assertEquals(2, summary.monthly.sumOf { it.count })
        assertTrue(summary.monthly.last().count >= 1)
    }

    @Test
    fun `session statistics use only the session queue`() {
        val photos = listOf(
            PhotoWithState(photo("a"), PhotoState(decision = PhotoDecision.KEPT)),
            PhotoWithState(
                photo("b"),
                PhotoState(decision = PhotoDecision.DELETE_CANDIDATE),
            ),
            PhotoWithState(photo("c"), PhotoState(favorite = true, decision = PhotoDecision.KEPT)),
            PhotoWithState(photo("outside"), PhotoState(decision = PhotoDecision.KEPT)),
        )
        val session = OrganizeSession(
            id = "session",
            queue = listOf("a", "b", "c"),
            cursor = 2,
            startedAt = now,
            filter = PhotoFilter(),
        )

        val summary = InsightsCalculator.calculate(photos, session = session, now = now)

        assertEquals(3, summary.session?.total)
        assertEquals(3, summary.session?.reviewed)
        assertEquals(1, summary.session?.markedForDeletion)
        assertEquals(1, summary.session?.favorites)
    }
}
