package app.photidy.core.localization

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware formatting helpers. UI code reads the active locale from
 * [android.content.res.Configuration] and passes it in, keeping this module free
 * of Compose dependencies.
 */
object PhotidyFormatters {

    fun formatDate(epochMillis: Long, locale: Locale): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(date)
    }

    fun formatMonthShort(year: Int, month: Int, locale: Locale): String {
        val monthObj = java.time.Month.of(month)
        return DateTimeFormatter.ofPattern("MMM", locale).format(monthObj).let {
            it.replace(".", "")
        }
    }

    fun formatNumber(value: Int, locale: Locale): String =
        String.format(locale, "%,d", value)

    fun formatPercentage(fraction: Float, locale: Locale): String =
        String.format(locale, "%d%%", (fraction * 100).toInt())

    fun formatBytes(bytes: Long, locale: Locale): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        val pattern = if (value >= 100) "%.0f %s" else "%.1f %s"
        return String.format(locale, pattern, value, units[index])
    }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainder = seconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, remainder)
    }

    fun formatDimensions(width: Int, height: Int): String = "$width × $height"
}
