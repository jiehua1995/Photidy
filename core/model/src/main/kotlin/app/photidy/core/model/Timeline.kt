package app.photidy.core.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Photos grouped by the month they were captured; the newest month comes first. */
data class TimelineSection(
    val year: Int,
    val month: Int,
    val photos: List<PhotoItem>,
) {
    /** False for photos without a usable capture date, which are shown last. */
    val isDated: Boolean
        get() = year > 0 && month in 1..12
}

object Timeline {

    const val UNKNOWN_YEAR = 0

    fun sections(
        items: List<PhotoWithState>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<TimelineSection> {
        if (items.isEmpty()) return emptyList()

        val dated = LinkedHashMap<YearMonth, MutableList<PhotoItem>>()
        val undated = mutableListOf<PhotoItem>()

        items.forEach { item ->
            val photo = item.photo
            if (photo.capturedAt <= 0L) {
                undated += photo
            } else {
                val month = YearMonth.from(Instant.ofEpochMilli(photo.capturedAt).atZone(zoneId))
                dated.getOrPut(month) { mutableListOf() } += photo
            }
        }

        val sections = dated.entries
            .sortedByDescending { it.key }
            .map { (month, photos) ->
                TimelineSection(
                    year = month.year,
                    month = month.monthValue,
                    photos = photos.sortedByDescending { it.capturedAt },
                )
            }
            .toMutableList()

        if (undated.isNotEmpty()) {
            sections += TimelineSection(
                year = UNKNOWN_YEAR,
                month = 0,
                photos = undated.sortedBy { it.name },
            )
        }
        return sections
    }
}
