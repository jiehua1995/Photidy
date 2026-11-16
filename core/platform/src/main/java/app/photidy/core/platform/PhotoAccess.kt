package app.photidy.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Photo/video read access, which Android splits by media type from API 33 and into
 * "selected photos only" from API 34. Photidy only ever asks for read access.
 */
object PhotoAccess {

    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"

    /** Permissions to request, matching the running Android version. */
    fun requestPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            READ_MEDIA_IMAGES,
            READ_MEDIA_VIDEO,
            READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            READ_MEDIA_IMAGES,
            READ_MEDIA_VIDEO,
        )

        else -> arrayOf(READ_EXTERNAL_STORAGE)
    }

    /** True when any photo or video can be listed, including a partial selection. */
    fun hasAnyAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            isGranted(context, READ_MEDIA_IMAGES) ||
                isGranted(context, READ_MEDIA_VIDEO) ||
                isGranted(context, READ_MEDIA_VISUAL_USER_SELECTED)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            isGranted(context, READ_MEDIA_IMAGES) || isGranted(context, READ_MEDIA_VIDEO)

        else -> isGranted(context, READ_EXTERNAL_STORAGE)
    }

    /** True only when the whole library is readable, i.e. not a partial selection. */
    fun hasFullAccess(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            isGranted(context, READ_MEDIA_IMAGES) && isGranted(context, READ_MEDIA_VIDEO)

        else -> isGranted(context, READ_EXTERNAL_STORAGE)
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
