package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PhotoFilterTest {

    private val zone = ZoneId.systemDefault()
    private val now = System.currentTimeMillis()

    private fun photo(
        id: String,
        albumId: String = "camera",
        daysAgo: Long = 1,
        sizeBytes: Long = 1_000_000,
        isVideo: Boolean = false,
    ): PhotoItem {
        val capturedAt = LocalDate.now(zone)
            .minusDays(daysAgo)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return PhotoItem(
            id = id,
            name = "IMG_$id",
            albumId = albumId,
            capturedAt = capturedAt,
            width = 4000,
            height = 3000,
            sizeBytes = sizeBytes,
            isVideo = isVideo,
            durationSeconds = if (isVideo) 20 else null,
            scene = PhotoScene.MOUNTAIN_DAWN,
            artSeed = id.hashCode(),
        )
    }

    private val library = listOf(
        PhotoWithState(photo("a", albumId = "camera", daysAgo = 2)),
        PhotoWithState(photo("b", albumId = "travel", daysAgo = 40, isVideo = true)),
        PhotoWithState(
            photo("c", albumId = "travel", daysAgo = 120, sizeBytes = 9_000_000),
            PhotoState(decision = PhotoDecision.KEPT),
        ),
        PhotoWithState(photo("d", albumId = "nature", daysAgo = 200, sizeBytes = 4_000_000)),
    )

    @Test
    fun `album filter only keeps matching photos`() {
        val result = PhotoFilter(albumId = "travel").applyTo(library, now)

        assertEquals(listOf("b", "c"), result.map { it.photo.id })
    }

    @Test
    fun `media filter separates photos and videos`() {
        val videos = PhotoFilter(media = MediaFilter.VIDEOS).applyTo(library, now)
        val photos = PhotoFilter(media = MediaFilter.PHOTOS).applyTo(library, now)

        assertEquals(listOf("b"), videos.map { it.photo.id })
        assertEquals(3, photos.size)
    }

    @Test
    fun `last thirty days excludes older photos`() {
        val result = PhotoFilter(dateRange = DateRangeFilter.LAST_30_DAYS).applyTo(library, now)

        assertEquals(listOf("a"), result.map { it.photo.id })
    }

    @Test
    fun `last ninety days includes photos inside the window`() {
        val ids = PhotoFilter(dateRange = DateRangeFilter.LAST_90_DAYS)
            .applyTo(library, now)
            .map { it.photo.id }

        assertTrue(ids.contains("a"))
        assertTrue(ids.contains("b"))
        assertTrue(!ids.contains("c"))
    }

    @Test
    fun `unorganized filter removes reviewed photos`() {
        val ids = PhotoFilter(unorganizedOnly = true).applyTo(library, now).map { it.photo.id }

        assertTrue(!ids.contains("c"))
        assertTrue(ids.contains("a"))
    }

    @Test
    fun `sorting orders by date or size`() {
        val newest = PhotoFilter(sort = PhotoSort.NEWEST_FIRST).applyTo(library, now)
        val oldest = PhotoFilter(sort = PhotoSort.OLDEST_FIRST).applyTo(library, now)
        val largest = PhotoFilter(sort = PhotoSort.LARGEST_FIRST).applyTo(library, now)

        assertEquals(listOf("a", "b", "c", "d"), newest.map { it.photo.id })
        assertEquals(listOf("d", "c", "b", "a"), oldest.map { it.photo.id })
        assertEquals("c", largest.first().photo.id)
    }

    @Test
    fun `combined filters are applied together`() {
        val result = PhotoFilter(
            albumId = "travel",
            dateRange = DateRangeFilter.LAST_90_DAYS,
        ).applyTo(library, now)

        assertEquals(listOf("b"), result.map { it.photo.id })
    }

    @Test
    fun `favorites filter only keeps favorited photos`() {
        val withFavorite = library.map { item ->
            if (item.photo.id == "d") {
                item.copy(state = item.state.copy(favorite = true))
            } else {
                item
            }
        }

        val ids = PhotoFilter(favoritesOnly = true)
            .applyTo(withFavorite, now)
            .map { it.photo.id }

        assertEquals(listOf("d"), ids)
    }
}
