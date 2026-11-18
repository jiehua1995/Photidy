package app.photidy.core.data

import android.content.IntentSender
import android.net.Uri
import app.photidy.core.model.Album
import app.photidy.core.model.AiModelTier
import app.photidy.core.model.AiTag
import app.photidy.core.model.GestureAction
import app.photidy.core.model.LibraryMode
import app.photidy.core.model.LibrarySnapshot
import app.photidy.core.model.PhotoDecision
import app.photidy.core.model.PhotoFilter
import app.photidy.core.model.PhotoItem
import app.photidy.core.model.PhotoSort
import app.photidy.core.model.PhotoState
import app.photidy.core.model.PhotoWithState
import app.photidy.core.model.SessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for the photo list and the user's review decisions.
 *
 * Two catalogs are supported: the deterministic demo library used before device access is
 * granted, and the read-only MediaStore catalog. Every action still only writes local review
 * state; no file is ever modified or deleted in this phase.
 */
/** Snapshot of the active photo source, used by Home and Settings. */
data class LibraryState(
    val mode: LibraryMode = LibraryMode.DEMO,
    val hasAccess: Boolean = false,
    /** False when only the per-item "selected photos" grant is held. */
    val hasFullAccess: Boolean = false,
    val devicePhotoCount: Int = 0,
    val deviceAlbumCount: Int = 0,
    val isScanning: Boolean = false,
)

/** A system-approved move of one device photo into another album folder. */
data class AlbumMove(
    val intentSender: IntentSender,
    val uris: List<Uri>,
    val albumId: String,
    val relativePath: String,
    val photoId: String,
)

class PhotoRepository(
    private val store: LibraryStore,
    private val mediaLibrary: MediaLibrary,
    private val mediaEditor: MediaEditor,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val demoPhotos: List<PhotoItem> = DemoLibrary.createPhotos(now())

    private val mode = MutableStateFlow(LibraryMode.DEMO)
    private val deviceLibrary = MutableStateFlow(DeviceLibrary())
    private val deviceScanning = MutableStateFlow(false)
    private val deviceAccess = MutableStateFlow(mediaLibrary.hasAccess())
    private val deviceFullAccess = MutableStateFlow(mediaLibrary.hasFullAccess())
    private val trash = MutableStateFlow(TrashSummary())

    /** Deleted media still restorable from Android's own trash. */
    val trashed: StateFlow<TrashSummary> = trash

    val snapshot: Flow<LibrarySnapshot> = store.snapshot

    val catalog: StateFlow<List<PhotoItem>> = combine(mode, deviceLibrary) { currentMode, device ->
        if (currentMode == LibraryMode.DEVICE) device.photos else demoPhotos
    }.stateIn(scope, SharingStarted.Eagerly, demoPhotos)

    val albums: StateFlow<List<Album>> = combine(mode, deviceLibrary) { currentMode, device ->
        if (currentMode == LibraryMode.DEVICE) device.albums else DemoLibrary.albums
    }.stateIn(scope, SharingStarted.Eagerly, DemoLibrary.albums)

    val libraryMode: StateFlow<LibraryMode> = mode

    val libraryState: StateFlow<LibraryState> = combine(
        mode,
        deviceLibrary,
        deviceScanning,
        deviceAccess,
        deviceFullAccess,
    ) { currentMode, device, scanning, access, fullAccess ->
        LibraryState(
            mode = currentMode,
            hasAccess = access,
            hasFullAccess = fullAccess,
            devicePhotoCount = device.photos.size,
            deviceAlbumCount = device.albums.size,
            isScanning = scanning,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = LibraryState(
            hasAccess = mediaLibrary.hasAccess(),
            hasFullAccess = mediaLibrary.hasFullAccess(),
        ),
    )

    val photos: Flow<List<PhotoWithState>> = combine(snapshot, catalog) { snapshot, photos ->
        snapshot.toPhotoWithStates(photos)
    }

    fun hasDeviceAccess(): Boolean = mediaLibrary.hasAccess()

    fun hasFullDeviceAccess(): Boolean = mediaLibrary.hasFullAccess()

    /**
     * Applies the persisted library preference. The device catalog is re-queried when access
     * is available, so a source switch or a permission grant is reflected immediately.
     */
    fun applyMode(newMode: LibraryMode) {
        mode.value = newMode
        deviceAccess.value = mediaLibrary.hasAccess()
        deviceFullAccess.value = mediaLibrary.hasFullAccess()
        if (newMode == LibraryMode.DEVICE && deviceAccess.value) {
            refreshDeviceLibrary()
        } else if (newMode == LibraryMode.DEMO) {
            trash.value = TrashSummary()
        }
    }

    /** Re-reads MediaStore. Safe to call repeatedly; concurrent scans collapse into one. */
    fun refreshDeviceLibrary() {
        deviceAccess.value = mediaLibrary.hasAccess()
        deviceFullAccess.value = mediaLibrary.hasFullAccess()
        if (!deviceAccess.value || deviceScanning.value) return
        scope.launch {
            deviceScanning.value = true
            try {
                // Pages arrive while the cursor is still open, so the first photos show up
                // long before a very large library finishes scanning.
                deviceLibrary.value = mediaLibrary.load { partial ->
                    deviceLibrary.value = partial
                }
                // Deletions land in Android's trash, so the count is refreshed with each scan.
                trash.value = mediaLibrary.trashedSummary()
            } finally {
                deviceScanning.value = false
            }
        }
    }

    /**
     * Builds the system delete request for real device photos. Demo entries are ignored because
     * they have no content URI; the caller decides what to show when the request is unsupported.
     */
    fun deleteRequest(ids: Set<String>): MediaRequest =
        mediaEditor.deleteRequest(contentUrisOf(ids))

    /** Called after the system confirmed the deletion: drops local state and re-scans. */
    suspend fun completeDeletion(ids: Set<String>) {
        if (ids.isEmpty()) return
        store.update { snapshot -> SessionEngine.forgetPhotos(snapshot, ids) }
        refreshDeviceLibrary()
    }

    /**
     * Prepares a system-approved album move for a real device photo. Demo photos and albums
     * without a MediaStore path keep the plan-only behaviour and return null.
     */
    fun albumMoveRequest(photoId: String, albumId: String): AlbumMove? {
        val photo = catalog.value.firstOrNull { it.id == photoId } ?: return null
        val uri = photo.contentUri?.let(Uri::parse) ?: return null
        val album = albums.value.firstOrNull { it.id == albumId } ?: return null
        val path = album.relativePath ?: return null
        val request = mediaEditor.writeRequest(listOf(uri))
        if (request !is MediaRequest.Confirmation) return null
        return AlbumMove(
            intentSender = request.intentSender,
            uris = request.uris,
            albumId = albumId,
            relativePath = path,
            photoId = photoId,
        )
    }

    /** Moves the photo after the user approved the write request, then advances the session. */
    suspend fun completeAlbumMove(move: AlbumMove): Boolean {
        val moved = mediaEditor.moveToAlbum(move.uris, move.relativePath) > 0
        if (moved) {
            store.update { snapshot ->
                if (snapshot.session?.currentPhotoId == move.photoId) {
                    SessionEngine.commit(snapshot, GestureAction.ADD_TO_ALBUM, move.albumId)
                } else {
                    snapshot
                }
            }
            refreshDeviceLibrary()
        }
        return moved
    }

    private fun contentUrisOf(ids: Set<String>): List<Uri> = catalog.value
        .asSequence()
        .filter { it.id in ids }
        .mapNotNull { it.contentUri?.let(Uri::parse) }
        .toList()

    /** Applies [filter], stores the resulting queue and returns its size. */
    suspend fun startSession(filter: PhotoFilter): Int {
        var count = 0
        store.update { snapshot ->
            val matched = filter.applyTo(snapshot.toPhotoWithStates(catalog.value), now())
            // Random order is resolved once here so the session stays stable afterwards.
            val ordered = if (filter.sort == PhotoSort.RANDOM) matched.shuffled() else matched
            val queue = ordered.map { it.photo.id }
            count = queue.size
            if (queue.isEmpty()) {
                snapshot
            } else {
                SessionEngine.startSession(snapshot, queue, filter, now())
            }
        }
        return count
    }

    suspend fun commitAction(action: GestureAction, albumId: String? = null) {
        store.update { snapshot ->
            SessionEngine.commit(snapshot, action, albumId)
        }
    }

    /** Applies one decision to every selected photo in the current queue. */
    suspend fun commitBatch(action: GestureAction, photoIds: Set<String>) {
        if (photoIds.isEmpty()) return
        store.update { snapshot ->
            SessionEngine.commitBatch(snapshot, photoIds, action)
        }
    }

    suspend fun undo(): Boolean {
        var undone = false
        store.update { snapshot ->
            if (snapshot.undoStack.isEmpty()) {
                snapshot
            } else {
                undone = true
                SessionEngine.undo(snapshot)
            }
        }
        return undone
    }

    suspend fun toggleFavorite(photoId: String) {
        updateState(photoId) { current ->
            current.copy(favorite = !current.favorite)
        }
    }

    suspend fun markForDeletion(photoId: String) {
        updateState(photoId) { current ->
            current.copy(decision = PhotoDecision.DELETE_CANDIDATE)
        }
    }

    /** Applies the deletion flag to several photos in one write. */
    suspend fun markManyForDeletion(photoIds: Set<String>) {
        if (photoIds.isEmpty()) return
        store.update { snapshot ->
            var states = snapshot.states
            photoIds.forEach { id ->
                val current = states[id] ?: PhotoState.Initial
                if (current.decision != PhotoDecision.DELETE_CANDIDATE) {
                    states = states + (id to current.copy(decision = PhotoDecision.DELETE_CANDIDATE))
                }
            }
            if (states == snapshot.states) snapshot else snapshot.copy(states = states)
        }
    }

    /** Clears the deletion flag without marking the photo as reviewed. */
    suspend fun clearDeleteFlags(photoIds: Set<String>) {
        if (photoIds.isEmpty()) return
        store.update { snapshot ->
            var states = snapshot.states
            photoIds.forEach { id ->
                val current = states[id] ?: return@forEach
                if (current.decision == PhotoDecision.DELETE_CANDIDATE) {
                    states = states + (id to current.copy(decision = PhotoDecision.UNREVIEWED))
                }
            }
            if (states == snapshot.states) snapshot else snapshot.copy(states = states)
        }
    }

    suspend fun markAsKept(photoId: String) {
        updateState(photoId) { current ->
            current.copy(decision = PhotoDecision.KEPT)
        }
    }

    suspend fun removeFromDeleteList(photoIds: Set<String>) {
        if (photoIds.isEmpty()) return
        store.update { snapshot ->
            var states = snapshot.states
            photoIds.forEach { id ->
                val current = snapshot.states[id] ?: PhotoState.Initial
                if (current.decision == PhotoDecision.DELETE_CANDIDATE) {
                    states = states + (id to current.copy(decision = PhotoDecision.KEPT))
                }
            }
            snapshot.copy(states = states)
        }
    }

    suspend fun clearDeleteList() {
        store.update { snapshot ->
            val deleteIds = snapshot.states
                .filterValues { it.decision == PhotoDecision.DELETE_CANDIDATE }
                .keys
            var states = snapshot.states
            deleteIds.forEach { id ->
                val current = snapshot.states.getValue(id)
                states = states + (id to current.copy(decision = PhotoDecision.KEPT))
            }
            snapshot.copy(states = states)
        }
    }

    /**
     * Stores coarse on-device labels for the photos that were just analyzed. [analyzedIds]
     * includes photos without a match so they are not decoded again next time.
     */
    suspend fun saveAiTags(
        analyzedIds: Set<String>,
        tagsByPhoto: Map<String, List<AiTag>>,
        tier: AiModelTier,
    ) {
        if (analyzedIds.isEmpty()) return
        store.update { snapshot ->
            var states = snapshot.states
            analyzedIds.forEach { id ->
                val current = states[id] ?: PhotoState.Initial
                val tags = tagsByPhoto[id].orEmpty().distinct()
                states = states + (id to current.copy(
                    aiTags = tags,
                    aiAnalyzed = true,
                    aiModelTier = tier,
                ))
            }
            snapshot.copy(states = states)
        }
    }

    /** Drops labels produced by a previous model tier so they can be regenerated. */
    suspend fun resetAiTags(photoIds: Set<String>) {
        if (photoIds.isEmpty()) return
        store.update { snapshot ->
            var states = snapshot.states
            photoIds.forEach { id ->
                val current = states[id] ?: return@forEach
                val next = current.copy(
                    aiTags = emptyList(),
                    aiAnalyzed = false,
                    aiModelTier = null,
                )
                states = if (next == PhotoState.Initial) {
                    states - id
                } else {
                    states + (id to next)
                }
            }
            snapshot.copy(states = states)
        }
    }

    private suspend fun updateState(photoId: String, transform: (PhotoState) -> PhotoState) {
        store.update { snapshot ->
            val current = snapshot.states[photoId] ?: PhotoState.Initial
            val next = transform(current)
            if (next == current) {
                snapshot
            } else if (next == PhotoState.Initial) {
                snapshot.copy(states = snapshot.states - photoId)
            } else {
                snapshot.copy(states = snapshot.states + (photoId to next))
            }
        }
    }
}
