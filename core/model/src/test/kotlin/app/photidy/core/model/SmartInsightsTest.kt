package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartInsightsTest {

    private val now = System.currentTimeMillis()

    private fun photo(
        id: String,
        name: String = "IMG_$id.jpg",
        albumTitle: String? = null,
        sizeBytes: Long = 1_000_000,
        width: Int = 4000,
        height: Int = 3000,
        isVideo: Boolean = false,
    ) = PhotoItem(
        id = id,
        name = name,
        albumId = "bucket:$id",
        capturedAt = now - id.hashCode().toLong(),
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        isVideo = isVideo,
        albumTitle = albumTitle,
    )

    @Test
    fun `screenshot names and buckets are detected`() {
        assertTrue(SmartInsights.isScreenshot(photo("a", name = "Screenshot_20261114.png")))
        assertTrue(SmartInsights.isScreenshot(photo("b", albumTitle = "Screenshots")))
        assertTrue(SmartInsights.isScreenshot(photo("c", name = "截屏_2026.png")))
        assertFalse(SmartInsights.isScreenshot(photo("d", name = "IMG_0042.jpg")))
    }

    @Test
    fun `videos are never treated as screenshots`() {
        assertFalse(
            SmartInsights.isScreenshot(
                photo("v", name = "Screenshot_recording.mp4", isVideo = true),
            ),
        )
    }

    @Test
    fun `photos with identical size and dimensions form duplicate groups`() {
        val photos = listOf(
            photo("a", sizeBytes = 2_000_000),
            photo("b", sizeBytes = 2_000_000),
            photo("c", sizeBytes = 3_000_000),
            photo("d", sizeBytes = 2_000_000, width = 2000, height = 1500),
        )

        val groups = SmartInsights.duplicateGroups(photos)

        assertEquals(1, groups.size)
        assertEquals(setOf("a", "b"), groups.single().mapTo(HashSet()) { it.id })
    }

    @Test
    fun `single photos and videos are excluded from duplicate groups`() {
        val photos = listOf(
            photo("a"),
            photo("v1", isVideo = true),
            photo("v2", isVideo = true),
        )

        assertTrue(SmartInsights.duplicateGroups(photos).isEmpty())
        assertTrue(SmartInsights.duplicateIds(photos).isEmpty())
    }

    @Test
    fun `screenshot filter keeps only screenshot-like photos`() {
        val items = listOf(
            PhotoWithState(photo("a", name = "Screenshot_1.png")),
            PhotoWithState(photo("b")),
        )

        val result = PhotoFilter(smart = SmartFilter.SCREENSHOTS).applyTo(items, now)

        assertEquals(listOf("a"), result.map { it.photo.id })
    }

    @Test
    fun `duplicate filter keeps every member of a duplicate group`() {
        val items = listOf(
            PhotoWithState(photo("a", sizeBytes = 5_000_000)),
            PhotoWithState(photo("b", sizeBytes = 5_000_000)),
            PhotoWithState(photo("c", sizeBytes = 1_000_000)),
        )

        val ids = PhotoFilter(smart = SmartFilter.POSSIBLE_DUPLICATES)
            .applyTo(items, now)
            .map { it.photo.id }

        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun `smart filters compose with album filters`() {
        val screenshot = photo("a", name = "Screenshot_1.png")
        val items = listOf(
            PhotoWithState(screenshot),
            PhotoWithState(screenshot.copy(id = "b", albumId = "travel")),
        )

        val result = PhotoFilter(
            albumId = screenshot.albumId,
            smart = SmartFilter.SCREENSHOTS,
        ).applyTo(items, now)

        assertEquals(listOf("a"), result.map { it.photo.id })
    }
}
