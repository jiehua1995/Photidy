package app.photidy.core.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import app.photidy.core.model.Album
import app.photidy.core.model.PhotoItem
import app.photidy.core.platform.PhotoAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One MediaStore row, detached from the cursor so the mapping stays testable. */
data class DeviceMediaRow(
    val mediaId: Long,
    val displayName: String?,
    val mimeType: String?,
    val capturedAt: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val durationSeconds: Int?,
    val bucketId: Long,
    val bucketName: String?,
    val contentUri: String,
    val relativePath: String? = null,
)

data class DeviceLibrary(
    val photos: List<PhotoItem> = emptyList(),
    val albums: List<Album> = emptyList(),
)

const val DEMO_ALBUM_UNKNOWN = "bucket:unknown"

/** Queries the device photo library read-only through MediaStore. */
class MediaLibrary(private val context: Context) {

    fun hasAccess(): Boolean = PhotoAccess.hasAnyAccess(context)

    fun hasFullAccess(): Boolean = PhotoAccess.hasFullAccess(context)

    suspend fun load(
        limit: Int = MAX_PHOTOS,
        onPage: ((DeviceLibrary) -> Unit)? = null,
    ): DeviceLibrary = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext DeviceLibrary()
        val rows = ArrayList<DeviceMediaRow>(minOf(limit, PAGE_SIZE))
        runCatching {
            queryRows(limit) { row ->
                rows += row
                // Publishing every page keeps the first photos visible long before a large
                // library has finished scanning.
                if (onPage != null && rows.size % PAGE_SIZE == 0) {
                    onPage(buildDeviceLibrary(rows, limit))
                }
            }
        }
        buildDeviceLibrary(rows, limit)
    }

    private fun queryRows(limit: Int, onRow: (DeviceMediaRow) -> Unit) {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Video.VideoColumns.DURATION,
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val order = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, " +
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            order,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val takenColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val typeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val bucketColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val mediaId = cursor.getLong(idColumn)
                val mediaType = if (typeColumn >= 0) cursor.getInt(typeColumn) else 0
                val mime = mimeColumn.takeIf { it >= 0 }?.let(cursor::getStringOrNull)
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ||
                    mime?.startsWith("video/") == true
                val takenAt = if (takenColumn >= 0) cursor.getLong(takenColumn) else 0L
                val addedAt = if (addedColumn >= 0) cursor.getLong(addedColumn) else 0L
                val durationMillis = if (isVideo && durationColumn >= 0) {
                    cursor.getLong(durationColumn)
                } else {
                    0L
                }
                onRow(
                    DeviceMediaRow(
                        mediaId = mediaId,
                        displayName = nameColumn.takeIf { it >= 0 }?.let(cursor::getStringOrNull),
                        mimeType = mime,
                        capturedAt = if (takenAt > 0L) takenAt else addedAt * 1_000L,
                        width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0,
                        height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0,
                        sizeBytes = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L,
                        isVideo = isVideo,
                        durationSeconds = if (isVideo && durationMillis > 0L) {
                            (durationMillis / 1_000L).toInt()
                        } else {
                            null
                        },
                        bucketId = if (bucketColumn >= 0) cursor.getLong(bucketColumn) else 0L,
                        bucketName = bucketNameColumn.takeIf { it >= 0 }
                            ?.let(cursor::getStringOrNull),
                        contentUri = ContentUris.withAppendedId(
                            // Delete/write requests only accept the typed media collections.
                            mediaCollection(isVideo),
                            mediaId,
                        ).toString(),
                        relativePath = relativePathColumn.takeIf { it >= 0 }
                            ?.let(cursor::getStringOrNull),
                    ),
                )
                count++
            }
        }
    }

    companion object {
        /** Rows published per page while a large device library is scanned. */
        const val PAGE_SIZE = 600
        const val MAX_PHOTOS = 20_000
        const val MAX_ALBUMS = 200
    }
}

private fun mediaCollection(isVideo: Boolean): Uri = if (isVideo) {
    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
}

private fun Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

/** Maps raw rows into the model layer. Pure so it can be unit-tested without Android. */
fun buildDeviceLibrary(rows: List<DeviceMediaRow>, limit: Int = MediaLibrary.MAX_PHOTOS): DeviceLibrary {
    if (rows.isEmpty()) return DeviceLibrary()
    val included = rows
        .sortedByDescending { it.capturedAt }
        .take(limit)

    val photos = included.map { row ->
        PhotoItem(
            id = "media:${row.mediaId}",
            name = row.displayName?.takeIf { it.isNotBlank() } ?: "Media ${row.mediaId}",
            albumId = albumIdOf(row.bucketId),
            capturedAt = row.capturedAt,
            width = row.width,
            height = row.height,
            sizeBytes = row.sizeBytes,
            isVideo = row.isVideo,
            durationSeconds = if (row.isVideo) row.durationSeconds else null,
            contentUri = row.contentUri,
            albumTitle = row.bucketName,
        )
    }

    val albums = included
        .groupBy { it.bucketId }
        .entries
        .sortedByDescending { it.value.size }
        .take(MediaLibrary.MAX_ALBUMS)
        .map { (bucketId, bucketRows) ->
            Album(
                id = albumIdOf(bucketId),
                title = bucketRows.firstNotNullOfOrNull { it.bucketName },
                relativePath = bucketRows.firstNotNullOfOrNull { it.relativePath },
            )
        }

    return DeviceLibrary(photos = photos, albums = albums)
}

fun albumIdOf(bucketId: Long): String =
    if (bucketId <= 0L) DEMO_ALBUM_UNKNOWN else "bucket:$bucketId"
