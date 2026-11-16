package app.photidy.core.data

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import app.photidy.core.platform.MediaWriteAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of asking the system to act on real media files. */
sealed interface MediaRequest {
    /** The user has to approve the operation in a system dialog first. */
    data class Confirmation(val intentSender: IntentSender, val uris: List<Uri>) : MediaRequest

    /** Android 10 and below cannot show the consolidated delete/write sheet. */
    data object Unsupported : MediaRequest
}

/**
 * Wraps the two user-consented MediaStore operations Photidy needs: deleting photos and moving
 * them into a new album folder. Every call results in an explicit system confirmation; the app
 * never deletes or moves anything silently.
 */
class MediaEditor(private val context: Context) {

    private companion object {
        const val TAG = "MediaEditor"
    }

    fun deleteRequest(uris: List<Uri>): MediaRequest {
        if (uris.isEmpty() || !MediaWriteAccess.supportsSystemConfirmation()) {
            return MediaRequest.Unsupported
        }
        return runCatching {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, uris)
            MediaRequest.Confirmation(pending.intentSender, uris)
        }.getOrElse { error ->
            Log.w(TAG, "System delete request rejected: ${error.javaClass.simpleName}")
            MediaRequest.Unsupported
        }
    }

    fun writeRequest(uris: List<Uri>): MediaRequest {
        if (uris.isEmpty() || !MediaWriteAccess.supportsSystemConfirmation()) {
            return MediaRequest.Unsupported
        }
        return runCatching {
            val pending = MediaStore.createWriteRequest(context.contentResolver, uris)
            MediaRequest.Confirmation(pending.intentSender, uris)
        }.getOrElse { error ->
            Log.w(TAG, "System write request rejected: ${error.javaClass.simpleName}")
            MediaRequest.Unsupported
        }
    }

    /** Moves already-approved media into [relativePath], e.g. `Pictures/Photidy/Family`. */
    suspend fun moveToAlbum(uris: List<Uri>, relativePath: String): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty() || !MediaWriteAccess.supportsSystemConfirmation()) return@withContext 0
        val target = relativePath.trimEnd('/') + "/"
        uris.count { uri ->
            val pending = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val moved = runCatching {
                context.contentResolver.update(uri, pending, null, null)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, target)
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null) > 0
            }.getOrDefault(false)
            moved
        }
    }
}
