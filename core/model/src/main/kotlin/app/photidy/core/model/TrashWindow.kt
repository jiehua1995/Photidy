package app.photidy.core.model

/** How long Android keeps deleted media in its trash before removing it for good. */
object TrashWindow {

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Whole days left before [expiresAtMillis]. Rounds up so "0 days left" is never shown while
     * the item can still be restored; returns 0 when the expiration is unknown or already past.
     */
    fun daysLeft(expiresAtMillis: Long, now: Long): Int {
        if (expiresAtMillis <= now) return 0
        val remaining = expiresAtMillis - now
        return ((remaining + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
    }
}
