package app.photidy.core.model

import java.time.Instant
import java.time.ZoneId

/**
 * Metadata-only search. Nothing is decoded and nothing leaves the device; the query is matched
 * against file names, albums and date fragments so results stay instant on large libraries.
 */
object PhotoSearch {

    fun search(items: List<PhotoWithState>, query: String): List<PhotoWithState> {
        val terms = query.trim().lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        return items
            .asSequence()
            .filter { item -> terms.all { term -> matches(item.photo, term) } }
            .sortedByDescending { it.photo.capturedAt }
            .toList()
    }

    private fun matches(photo: PhotoItem, term: String): Boolean {
        val haystack = haystack(photo)
        if (haystack.contains(term)) return true
        // Separators are easy to forget on a phone keyboard, so IMG1003 also finds IMG_1003.
        val compactTerm = compact(term)
        return compactTerm.isNotEmpty() && compact(haystack).contains(compactTerm)
    }

    private fun haystack(photo: PhotoItem): String = buildString {
        append(photo.name.lowercase()).append(' ')
        photo.albumTitle?.let { append(it.lowercase()).append(' ') }
        append(photo.albumId.lowercase()).append(' ')
        append(dateTokens(photo.capturedAt))
    }

    /** Accepts 2026-11-17, 2026/11/17, 20261117 and 2026年11月17日 style fragments. */
    private fun dateTokens(capturedAt: Long): String {
        if (capturedAt <= 0L) return ""
        val date = Instant.ofEpochMilli(capturedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val monthPadded = month.toString().padStart(2, '0')
        val dayPadded = day.toString().padStart(2, '0')
        return "$year-$monthPadded-$dayPadded $year/$month/$day " +
            "$year$monthPadded$dayPadded ${year}年${month}月${day}日 " +
            "${year}年${month}月 ${month}月"
    }

    private val WHITESPACE = Regex("\\s+")

    private fun compact(value: String): String = value.filter { it.isLetterOrDigit() }
}
