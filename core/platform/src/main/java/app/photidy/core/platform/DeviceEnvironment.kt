package app.photidy.core.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Reads device-level conditions that should soften expensive visual effects.
 * We never overwrite the user's saved appearance choice; these flags only
 * reduce rendering cost for the current session.
 */
object DeviceEnvironment {

    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isPowerSaveMode
    }

    fun isReduceMotionEnabled(context: Context): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return scale == 0f
    }

    fun isLowRamDevice(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        return activityManager.isLowRamDevice
    }

    fun shouldReduceEffects(context: Context): Boolean =
        isPowerSaveMode(context) || isReduceMotionEnabled(context) || isLowRamDevice(context)
}
