package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PhotoSearchTest {

    private val zone = ZoneId.systemDefault()

    private fun capturedOn(year: Int, month: Int, day: Int): Long = LocalDate.of(year, month, day)
        .atTime(12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private fun item(
        id: String,
        name: String,
        albumId: String = "camera",
        albumTitle: String? = null,
        capturedAt: Long = capturedOn(2026, 11, 17),
    ) = PhotoWithState(
        PhotoItem(
            id = id,
            name = name,
            albumId = albumId,
            albumTitle = albumTitle,
            capturedAt = capturedAt,
            width = 4000,
            height = 3000,
            sizeBytes = 1_000_000,
        ),
    )

    private val library = listOf(
        item("a", name = "IMG_1001.jpg"),
        item("b", name = "IMG_2048.jpg", albumId = "bucket:1", albumTitle = "Screenshots"),
        item("c", name = "旅行_01.jpg", albumId = "travel", capturedAt = capturedOn(2025, 5, 4)),
    )

    @Test
    fun `matches file names case insensitively`() {
        val ids = PhotoSearch.search(library, "img_1001").map { it.photo.id }

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `matches names when separators are omitted`() {
        val ids = PhotoSearch.search(library, "IMG1001").map { it.photo.id }

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `matches bucket display names`() {
        val ids = PhotoSearch.search(library, "screen").map { it.photo.id }

        assertEquals(listOf("b"), ids)
    }

    @Test
    fun `matches album ids`() {
        val ids = PhotoSearch.search(library, "travel").map { it.photo.id }

        assertEquals(listOf("c"), ids)
    }

    @Test
    fun `matches date fragments in several formats`() {
        assertTrue(PhotoSearch.search(library, "2026-11").any { it.photo.id == "a" })
        assertTrue(PhotoSearch.search(library, "2026年11月").any { it.photo.id == "a" })
        assertTrue(PhotoSearch.search(library, "2025/5").any { it.photo.id == "c" })
    }

    @Test
    fun `multiple terms must all match`() {
        assertEquals(1, PhotoSearch.search(library, "img_1001 2026").size)
        assertTrue(PhotoSearch.search(library, "img_1001 2025").isEmpty())
    }

    @Test
    fun `blank query returns no results`() {
        assertTrue(PhotoSearch.search(library, "   ").isEmpty())
    }

    @Test
    fun `results are newest first`() {
        val ids = PhotoSearch.search(library, "202").map { it.photo.id }

        assertEquals(listOf("a", "b", "c"), ids)
    }
}
