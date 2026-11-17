package app.photidy.core.model

import kotlinx.serialization.Serializable

/** Optional metadata-only filters that surface likely clean-up candidates. */
@Serializable
enum class SmartFilter {
    NONE,
    SCREENSHOTS,
    POSSIBLE_DUPLICATES,
    BURST_SHOTS,
    LONG_VIDEOS,
    LARGE_FILES,
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

    /**
     * Flags consecutive photos from one album that share dimensions and were taken within a
     * few seconds of each other. This is the metadata signature of a burst, without decoding
     * a single pixel.
     */
    fun burstIds(photos: List<PhotoItem>): Set<String> {
        val result = HashSet<String>()
        val group = ArrayList<PhotoItem>()

        fun flush() {
            if (group.size >= 2) group.forEach { result += it.id }
            group.clear()
        }

        photos.asSequence()
            .filter { !it.isVideo && it.capturedAt > 0L && it.width > 0 && it.height > 0 }
            .sortedBy { it.capturedAt }
            .forEach { photo ->
                val previous = group.lastOrNull()
                val continuesBurst = previous != null &&
                    previous.albumId == photo.albumId &&
                    previous.width == photo.width &&
                    previous.height == photo.height &&
                    photo.capturedAt - previous.capturedAt <= BURST_GAP_MILLIS
                if (!continuesBurst) flush()
                group += photo
            }
        flush()
        return result
    }

    fun isLongVideo(photo: PhotoItem): Boolean =
        photo.isVideo && (photo.durationSeconds ?: 0) >= LONG_VIDEO_SECONDS

    fun isLargeFile(photo: PhotoItem): Boolean = photo.sizeBytes >= LARGE_FILE_BYTES

    data class DuplicateKey(val sizeBytes: Long, val width: Int, val height: Int)

    private const val BURST_GAP_MILLIS = 3_000L
    private const val LONG_VIDEO_SECONDS = 60
    private const val LARGE_FILE_BYTES = 20L * 1024 * 1024
}
