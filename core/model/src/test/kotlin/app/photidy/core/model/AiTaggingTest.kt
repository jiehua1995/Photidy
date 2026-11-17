package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTaggingTest {

    private val now = System.currentTimeMillis()

    private fun photo(id: String) = PhotoItem(
        id = id,
        name = "IMG_$id.jpg",
        albumId = "camera",
        capturedAt = now,
        width = 4000,
        height = 3000,
        sizeBytes = 1_000_000,
    )

    @Test
    fun `common labels map to friendly categories`() {
        assertEquals(listOf(AiTag.FOOD), AiTagging.tagsFor(listOf("pizza")))
        assertEquals(listOf(AiTag.PET), AiTagging.tagsFor(listOf("Maltese dog")))
        assertEquals(listOf(AiTag.DOCUMENT), AiTagging.tagsFor(listOf("web site")))
        assertEquals(listOf(AiTag.NATURE), AiTagging.tagsFor(listOf("seashore")))
        assertEquals(listOf(AiTag.LANDMARK), AiTagging.tagsFor(listOf("castle")))
    }

    @Test
    fun `food labels are not mistaken for pets`() {
        assertEquals(listOf(AiTag.FOOD), AiTagging.tagsFor(listOf("hotdog")))
    }

    @Test
    fun `unknown or empty labels produce no tags`() {
        assertTrue(AiTagging.tagsFor(emptyList()).isEmpty())
        assertTrue(AiTagging.tagsFor(listOf("tench", "school bus")).isEmpty())
    }

    @Test
    fun `multiple labels can produce multiple tags`() {
        val tags = AiTagging.tagsFor(listOf("pizza", "seashore"))

        assertEquals(listOf(AiTag.FOOD, AiTag.NATURE), tags)
    }

    @Test
    fun `ai tag filter keeps only analyzed photos with that tag`() {
        val items = listOf(
            PhotoWithState(
                photo("food"),
                PhotoState(aiTags = listOf(AiTag.FOOD), aiAnalyzed = true),
            ),
            PhotoWithState(
                photo("pet"),
                PhotoState(aiTags = listOf(AiTag.PET), aiAnalyzed = true),
            ),
            PhotoWithState(photo("unanalyzed")),
        )

        val ids = PhotoFilter(aiTag = AiTag.FOOD).applyTo(items, now).map { it.photo.id }

        assertEquals(listOf("food"), ids)
    }
}
