package app.photidy.core.model

import kotlinx.serialization.Serializable

/**
 * Visual scene used by the procedural demo artwork. Real media will replace this
 * when MediaStore integration lands; the rest of the app already consumes PhotoItem.
 */
@Serializable
enum class PhotoScene {
    MOUNTAIN_DAWN,
    OCEAN_DUSK,
    FOREST_MIST,
    DESERT_DUNES,
    CITY_NIGHT,
    SNOW_PEAK,
    MEADOW_LIGHT,
    LAKE_REFLECTION,
    WARM_TABLE,
    SOFT_PORTRAIT,
}

@Serializable
data class Album(
    val id: String,
    /** Bucket name reported by MediaStore; null for the bundled demo albums. */
    val title: String? = null,
    /** MediaStore RELATIVE_PATH of the bucket; null when it is not writable. */
    val relativePath: String? = null,
)

@Serializable
data class PhotoItem(
    val id: String,
    val name: String,
    val albumId: String,
    val capturedAt: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isVideo: Boolean = false,
    val durationSeconds: Int? = null,
    val artSeed: Int = 0,
    val scene: PhotoScene = PhotoScene.MOUNTAIN_DAWN,
    val paletteIndex: Int = 0,
    /** Content URI of a real device photo or video; null for procedural demo artwork. */
    val contentUri: String? = null,
    /** MediaStore bucket name for real media, so album chips can show it verbatim. */
    val albumTitle: String? = null,
) {
    val aspectRatio: Float
        get() = if (width <= 0 || height <= 0) 1f else width.toFloat() / height.toFloat()
}

@Serializable
enum class PhotoDecision {
    UNREVIEWED,
    KEPT,
    DELETE_CANDIDATE,
}

@Serializable
data class PhotoState(
    val decision: PhotoDecision = PhotoDecision.UNREVIEWED,
    val favorite: Boolean = false,
    val albumOverrideId: String? = null,
    /** Coarse labels from the optional on-device model; empty until analyzed. */
    val aiTags: List<AiTag> = emptyList(),
    /** True once the on-device model has looked at the photo, even without a match. */
    val aiAnalyzed: Boolean = false,
    /** Tier that produced [aiTags]; null before any model ran. */
    val aiModelTier: AiModelTier? = null,
) {
    val isReviewed: Boolean
        get() = decision != PhotoDecision.UNREVIEWED

    companion object {
        val Initial = PhotoState()
    }
}

data class PhotoWithState(
    val photo: PhotoItem,
    val state: PhotoState = PhotoState.Initial,
)
