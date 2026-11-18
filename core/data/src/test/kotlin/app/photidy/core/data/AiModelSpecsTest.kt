package app.photidy.core.data

import app.photidy.core.model.AiModelTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelSpecsTest {

    @Test
    fun everyTierHasItsOwnModelFile() {
        val files = AiModelTier.entries.map { AiModelSpecs.of(it).fileName }
        assertEquals(AiModelTier.entries.size, files.toSet().size)
    }

    @Test
    fun onlyTheLightTierUsesAnArchive() {
        assertNotNull(AiModelSpecs.of(AiModelTier.LIGHT).archiveEntry)
        assertNull(AiModelSpecs.of(AiModelTier.BALANCED).archiveEntry)
        assertNull(AiModelSpecs.of(AiModelTier.ACCURATE).archiveEntry)
    }

    @Test
    fun downloadsAreHttpsAndSizesGrowWithQuality() {
        val specs = AiModelTier.entries.map(AiModelSpecs::of)
        specs.forEach { assertTrue(it.url.startsWith("https://")) }
        assertTrue(specs[0].minBytes < specs[1].minBytes)
        assertTrue(specs[1].minBytes < specs[2].minBytes)
    }
}
