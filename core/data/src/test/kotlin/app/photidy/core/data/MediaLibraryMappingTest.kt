package app.photidy.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryMappingTest {

    private fun row(
        mediaId: Long,
        bucketId: Long = 100L,
        bucketName: String? = "Camera",
        capturedAt: Long = mediaId,
        isVideo: Boolean = false,
        durationSeconds: Int? = null,
        displayName: String? = "IMG_$mediaId.jpg",
        width: Int = 4_000,
        height: Int = 3_000,
        sizeBytes: Long = 2_000_000L,
    ) = DeviceMediaRow(
        mediaId = mediaId,
        displayName = displayName,
        mimeType = if (isVideo) "video/mp4" else "image/jpeg",
        capturedAt = capturedAt,
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        isVideo = isVideo,
        durationSeconds = durationSeconds,
        bucketId = bucketId,
        bucketName = bucketName,
        contentUri = "content://media/external/file/$mediaId",
    )

    @Test
    fun `empty rows produce an empty device library`() {
        val library = buildDeviceLibrary(emptyList())

        assertTrue(library.photos.isEmpty())
        assertTrue(library.albums.isEmpty())
    }

    @Test
    fun `rows map to photo items with device metadata`() {
        val library = buildDeviceLibrary(listOf(row(mediaId = 7, bucketId = 42, bucketName = "Trips")))
        val photo = library.photos.single()

        assertEquals("media:7", photo.id)
        assertEquals("IMG_7.jpg", photo.name)
        assertEquals("bucket:42", photo.albumId)
        assertEquals("Trips", photo.albumTitle)
        assertEquals("content://media/external/file/7", photo.contentUri)
        assertEquals(4_000, photo.width)
        assertEquals(3_000, photo.height)
        assertEquals(2_000_000L, photo.sizeBytes)
    }

    @Test
    fun `video rows keep duration while photos drop it`() {
        val library = buildDeviceLibrary(
            listOf(
                row(mediaId = 1, isVideo = true, durationSeconds = 12),
                row(mediaId = 2, durationSeconds = 12),
            ),
        )

        val video = library.photos.first { it.isVideo }
        val photo = library.photos.first { !it.isVideo }

        assertTrue(video.isVideo)
        assertEquals(12, video.durationSeconds)
        assertNull(photo.durationSeconds)
    }

    @Test
    fun `blank display names fall back to a stable media label`() {
        val library = buildDeviceLibrary(listOf(row(mediaId = 9, displayName = "  ")))

        assertEquals("Media 9", library.photos.single().name)
    }

    @Test
    fun `albums aggregate by bucket and sort by photo count`() {
        val rows = listOf(
            row(mediaId = 1, bucketId = 10, bucketName = "Camera"),
            row(mediaId = 2, bucketId = 20, bucketName = "Screenshots"),
            row(mediaId = 3, bucketId = 20, bucketName = "Screenshots"),
            row(mediaId = 4, bucketId = 20, bucketName = "Screenshots"),
        )

        val albums = buildDeviceLibrary(rows).albums

        assertEquals(listOf("bucket:20", "bucket:10"), albums.map { it.id })
        assertEquals(listOf("Screenshots", "Camera"), albums.map { it.title })
    }

    @Test
    fun `newest photos win when the limit truncates`() {
        val rows = listOf(
            row(mediaId = 1, capturedAt = 100L),
            row(mediaId = 2, capturedAt = 300L),
            row(mediaId = 3, capturedAt = 200L),
        )

        val library = buildDeviceLibrary(rows, limit = 2)

        assertEquals(listOf("media:2", "media:3"), library.photos.map { it.id })
    }

    @Test
    fun `missing bucket ids use the shared unknown album`() {
        val library = buildDeviceLibrary(listOf(row(mediaId = 5, bucketId = 0L, bucketName = null)))

        assertEquals(DEMO_ALBUM_UNKNOWN, library.photos.single().albumId)
        assertEquals(DEMO_ALBUM_UNKNOWN, library.albums.single().id)
        assertNull(library.albums.single().title)
    }
}
