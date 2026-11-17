package app.photidy.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
enum class MediaFilter {
    ALL,
    PHOTOS,
    VIDEOS,
}

@Serializable
enum class DateRangeFilter {
    ALL_TIME,
    LAST_30_DAYS,
    LAST_90_DAYS,
    THIS_YEAR,
}

@Serializable
enum class PhotoSort {
    NEWEST_FIRST,
    OLDEST_FIRST,
    LARGEST_FIRST,
    RANDOM,
}

@Serializable
data class PhotoFilter(
    val albumId: String? = null,
    val media: MediaFilter = MediaFilter.ALL,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val unorganizedOnly: Boolean = false,
    val sort: PhotoSort = PhotoSort.NEWEST_FIRST,
    val smart: SmartFilter = SmartFilter.NONE,
    val favoritesOnly: Boolean = false,
) {
    fun matches(item: PhotoWithState, now: Long): Boolean {
        val photo = item.photo
        if (albumId != null && photo.albumId != albumId) return false
        when (media) {
            MediaFilter.ALL -> Unit
            MediaFilter.PHOTOS -> if (photo.isVideo) return false
            MediaFilter.VIDEOS -> if (!photo.isVideo) return false
        }
        if (unorganizedOnly && item.state.isReviewed) return false
        if (favoritesOnly && !item.state.favorite) return false
        if (!isWithinDateRange(photo.capturedAt, now)) return false
        return true
    }

    fun applyTo(items: List<PhotoWithState>, now: Long): List<PhotoWithState> {
        var filtered = items.filter { matches(it, now) }
        filtered = when (smart) {
            SmartFilter.NONE -> filtered
            SmartFilter.SCREENSHOTS -> filtered.filter { SmartInsights.isScreenshot(it.photo) }
            SmartFilter.POSSIBLE_DUPLICATES -> {
                val ids = SmartInsights.duplicateIds(filtered.map { it.photo })
                filtered.filter { it.photo.id in ids }
            }
        }
        return when (sort) {
            PhotoSort.NEWEST_FIRST -> filtered.sortedByDescending { it.photo.capturedAt }
            PhotoSort.OLDEST_FIRST -> filtered.sortedBy { it.photo.capturedAt }
            PhotoSort.LARGEST_FIRST -> filtered.sortedByDescending { it.photo.sizeBytes }
            // Shuffling happens once when the session starts, so the preview stays stable.
            PhotoSort.RANDOM -> filtered
        }
    }

    private fun isWithinDateRange(capturedAt: Long, now: Long): Boolean {
        if (capturedAt > now) return false
        return when (dateRange) {
            DateRangeFilter.ALL_TIME -> true
            DateRangeFilter.LAST_30_DAYS -> capturedAt >= now - THIRTY_DAYS_MILLIS
            DateRangeFilter.LAST_90_DAYS -> capturedAt >= now - NINETY_DAYS_MILLIS
            DateRangeFilter.THIS_YEAR -> {
                val zone = ZoneId.systemDefault()
                val capturedYear = Instant.ofEpochMilli(capturedAt).atZone(zone).year
                val currentYear = Instant.ofEpochMilli(now).atZone(zone).year
                capturedYear == currentYear
            }
        }
    }

    private companion object {
        const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val NINETY_DAYS_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
