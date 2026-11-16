package app.photidy.core.data

import app.photidy.core.model.Album
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoScene
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Deterministic demo library. Photos are procedural, so the prototype never depends on
 * bundled binaries or network images. Real MediaStore data will replace this later.
 */
object DemoLibrary {

    val albums: List<Album> = listOf(
        Album("camera"),
        Album("travel"),
        Album("family"),
        Album("nature"),
        Album("city"),
        Album("food"),
        Album("pets"),
    )

    private val resolutions = listOf(
        4032 to 3024,
        3024 to 4032,
        3840 to 2160,
        2160 to 3840,
        3024 to 3024,
        4000 to 3000,
        3000 to 4000,
        1920 to 1080,
    )

    private val albumCycle = listOf(
        "camera", "travel", "family", "nature", "nature",
        "city", "food", "pets", "camera", "travel",
        "family", "city", "nature", "camera", "pets", "food",
    )

    private val scenesByAlbum = mapOf(
        "camera" to listOf(PhotoScene.MOUNTAIN_DAWN, PhotoScene.LAKE_REFLECTION, PhotoScene.MEADOW_LIGHT),
        "travel" to listOf(PhotoScene.OCEAN_DUSK, PhotoScene.LAKE_REFLECTION, PhotoScene.DESERT_DUNES),
        "family" to listOf(PhotoScene.MEADOW_LIGHT, PhotoScene.SOFT_PORTRAIT, PhotoScene.WARM_TABLE),
        "nature" to listOf(PhotoScene.FOREST_MIST, PhotoScene.SNOW_PEAK, PhotoScene.LAKE_REFLECTION),
        "city" to listOf(PhotoScene.CITY_NIGHT, PhotoScene.CITY_NIGHT, PhotoScene.MOUNTAIN_DAWN),
        "food" to listOf(PhotoScene.WARM_TABLE, PhotoScene.WARM_TABLE, PhotoScene.MEADOW_LIGHT),
        "pets" to listOf(PhotoScene.SOFT_PORTRAIT, PhotoScene.SOFT_PORTRAIT, PhotoScene.MEADOW_LIGHT),
    )

    fun createPhotos(now: Long, count: Int = 56): List<PhotoItem> {
        val zone = ZoneId.systemDefault()
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val items = ArrayList<PhotoItem>(count)
        for (index in 0 until count) {
            val seed = 1200 + index * 37
            val random = Random(seed)
            val albumId = albumCycle[index % albumCycle.size]
            val albumIndex = albums.indexOfFirst { it.id == albumId }.coerceAtLeast(0)
            val isVideo = index % 9 == 4
            val (width, height) = if (isVideo) {
                if (index % 2 == 0) 3840 to 2160 else 2160 to 3840
            } else {
                resolutions[(index + albumIndex) % resolutions.size]
            }

            val daysAgo = when {
                index < 8 -> index + 1L
                else -> ((index * 9L) % 500L) + 2L
            }
            val hour = 7 + (index * 3) % 14
            val minute = (index * 11) % 60
            val capturedAt = today.minusDays(daysAgo)
                .atTime(hour, minute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            val durationSeconds = if (isVideo) 12 + random.nextInt(84) else null
            val sizeBytes = if (isVideo) {
                durationSeconds!! * 1_450_000L + random.nextInt(4_000_000)
            } else {
                (width.toLong() * height.toLong()) / 4 + random.nextInt(2_200_000)
            }

            items += PhotoItem(
                id = "photo_%03d".format(index),
                name = if (isVideo) "VID_%04d".format(1000 + index) else "IMG_%04d".format(1000 + index),
                albumId = albumId,
                capturedAt = capturedAt,
                width = width,
                height = height,
                sizeBytes = sizeBytes,
                isVideo = isVideo,
                durationSeconds = durationSeconds,
                artSeed = seed,
                scene = scenesByAlbum.getValue(albumId)[index % 3],
                paletteIndex = (index + albumIndex) % 8,
            )
        }
        return items
    }
}
