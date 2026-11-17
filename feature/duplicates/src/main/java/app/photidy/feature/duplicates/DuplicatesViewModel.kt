package app.photidy.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.DuplicateFinder
import app.photidy.core.model.PhotoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicateGroupUi(
    val key: String,
    val photos: List<PhotoItem>,
    val keepId: String,
) {
    val others: List<PhotoItem>
        get() = photos.filterNot { it.id == keepId }

    val reclaimableBytes: Long
        get() = others.sumOf { it.sizeBytes }
}

data class AppliedGroup(
    val flaggedIds: Set<String>,
)

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val groups: List<DuplicateGroupUi> = emptyList(),
    val totalGroups: Int = 0,
    val reclaimableBytes: Long = 0L,
    val skippedCount: Int = 0,
    /** Non-null right after a group is applied, so the screen can offer an undo. */
    val lastApplied: AppliedGroup? = null,
)

class DuplicatesViewModel : ViewModel() {

    private val photoRepository = PhotidyGraph.photoRepository

    /** Explicit keeper per group; groups without an entry fall back to the suggestion. */
    private val selection = MutableStateFlow<Map<String, String>>(emptyMap())
    private val skipped = MutableStateFlow<Set<String>>(emptySet())
    private val lastApplied = MutableStateFlow<AppliedGroup?>(null)

    val uiState: StateFlow<DuplicatesUiState> = combine(
        photoRepository.photos,
        selection,
        skipped,
        lastApplied,
    ) { photos, selections, skippedKeys, applied ->
        val groups = DuplicateFinder.pendingGroups(photos).map { group ->
            val key = group.photos.joinToString("+") { it.id }
            val chosen = selections[key]?.takeIf { id -> group.photos.any { it.id == id } }
            DuplicateGroupUi(
                key = key,
                photos = group.photos,
                keepId = chosen ?: group.suggestedKeepId,
            )
        }
        DuplicatesUiState(
            isLoading = false,
            groups = groups.filterNot { it.key in skippedKeys },
            totalGroups = groups.size,
            reclaimableBytes = groups.sumOf { it.reclaimableBytes },
            skippedCount = skippedKeys.size,
            lastApplied = applied,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DuplicatesUiState(),
    )

    fun chooseKeeper(groupKey: String, photoId: String) {
        selection.update { it + (groupKey to photoId) }
    }

    fun skip(groupKey: String) {
        skipped.update { it + groupKey }
    }

    fun restoreSkipped() {
        skipped.value = emptySet()
    }

    /** Keeps the chosen photo and flags every other member for deletion. */
    fun applyGroup(groupKey: String) {
        val group = uiState.value.groups.firstOrNull { it.key == groupKey } ?: return
        val otherIds = group.others.mapTo(HashSet()) { it.id }
        if (otherIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.markManyForDeletion(otherIds)
            selection.update { it - groupKey }
            lastApplied.value = AppliedGroup(otherIds)
        }
    }

    /** Reverts the last group so its photos can be compared again. */
    fun undoLast() {
        val applied = lastApplied.value ?: return
        viewModelScope.launch {
            photoRepository.clearDeleteFlags(applied.flaggedIds)
            lastApplied.value = null
        }
    }
}
