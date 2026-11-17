package app.photidy.core.model

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun photo(id: String, capturedAt: Long) = PhotoItem(
        id = id,
        name = "IMG_$id.jpg",
        albumId = "camera",
        capturedAt = capturedAt,
        width = 4000,
        height = 3000,
        sizeBytes = 2_000_000,
    )

    @Test
    fun `months are ordered newest first`() {
        val items = listOf(
            PhotoWithState(photo("sep", millis(2026, 9, 3))),
            PhotoWithState(photo("nov", millis(2026, 11, 2))),
            PhotoWithState(photo("oct", millis(2026, 10, 20))),
        )

        val sections = Timeline.sections(items, zone)

        assertEquals(listOf(11, 10, 9), sections.map { it.month })
        assertTrue(sections.all { it.isDated })
    }

    @Test
    fun `photos inside a month are newest first`() {
        val items = listOf(
            PhotoWithState(photo("early", millis(2026, 11, 2, 8))),
            PhotoWithState(photo("late", millis(2026, 11, 2, 20))),
        )

        val section = Timeline.sections(items, zone).single()

        assertEquals(listOf("late", "early"), section.photos.map { it.id })
    }

    @Test
    fun `undated photos are collected in a final bucket`() {
        val items = listOf(
            PhotoWithState(photo("dated", millis(2026, 11, 2))),
            PhotoWithState(photo("b", 0L)),
            PhotoWithState(photo("a", -1L)),
        )

        val sections = Timeline.sections(items, zone)

        assertEquals(2, sections.size)
        val unknown = sections.last()
        assertTrue(!unknown.isDated)
        assertEquals(listOf("a", "b"), unknown.photos.map { it.id })
    }

    @Test
    fun `empty input produces no sections`() {
        assertTrue(Timeline.sections(emptyList(), zone).isEmpty())
    }
}
