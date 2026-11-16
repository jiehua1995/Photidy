package app.photidy.core.platform

import android.os.Build

/**
 * MediaStore's user-consented delete/write requests (`createDeleteRequest` /
 * `createWriteRequest`) exist from Android 11. Older versions keep the plan-only flow.
 */
object MediaWriteAccess {
    fun supportsSystemConfirmation(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}
