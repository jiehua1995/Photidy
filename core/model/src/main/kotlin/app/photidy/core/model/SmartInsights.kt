package app.photidy.core.model

import kotlinx.serialization.Serializable

/** Optional metadata-only filters that surface likely clean-up candidates. */
@Serializable
enum class SmartFilter {
    NONE,
    SCREENSHOTS,
    POSSIBLE_DUPLICATES,
}

/**
 * Metadata-only heuristics for local suggestions. Nothing is decoded and no file is touched,
 * so these stay instant on large libraries. Results are suggestions, never automatic actions.
 */
object SmartInsights {

    private val screenshotHints = listOf(
        "screenshot",
        "screen_shot",
        "screen-shot",
        "scr_",
        "截屏",
        "截图",
    )

    fun isScreenshot(photo: PhotoItem): Boolean {
        if (photo.isVideo) return false
        val haystack = listOfNotNull(photo.name, photo.albumTitle)
            .joinToString(" ")
            .lowercase()
        return screenshotHints.any { haystack.contains(it) }
    }

    /**
     * Groups photos whose file size and pixel dimensions match exactly. Metadata is used instead
     * of pixel hashing so the check stays cheap and deterministic; perceptual duplicates can be
     * layered on later without changing the call sites.
     */
    fun duplicateGroups(photos: List<PhotoItem>): List<List<PhotoItem>> = photos
        .asSequence()
        .filter { !it.isVideo && it.sizeBytes > 0 && it.width > 0 && it.height > 0 }
        .groupBy { DuplicateKey(it.sizeBytes, it.width, it.height) }
        .values
        .filter { it.size >= 2 }
        .sortedByDescending { it.size }

    fun duplicateIds(photos: List<PhotoItem>): Set<String> =
        duplicateGroups(photos).flatten().mapTo(HashSet()) { it.id }

    data class DuplicateKey(val sizeBytes: Long, val width: Int, val height: Int)
}
