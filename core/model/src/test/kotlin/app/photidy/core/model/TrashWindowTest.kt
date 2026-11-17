package app.photidy.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrashWindowTest {

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `partial days round up so a restorable item never shows zero`() {
        assertEquals(1, TrashWindow.daysLeft(expiresAtMillis = 1_000L, now = 0L))
    }

    @Test
    fun `full days are reported as they are`() {
        assertEquals(3, TrashWindow.daysLeft(expiresAtMillis = 3 * day, now = 0L))
    }

    @Test
    fun `unknown or past expiry reports zero`() {
        assertEquals(0, TrashWindow.daysLeft(expiresAtMillis = 0L, now = 100L))
        assertEquals(0, TrashWindow.daysLeft(expiresAtMillis = 50L, now = 100L))
    }
}
