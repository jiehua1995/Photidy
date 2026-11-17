package app.photidy.core.model

/** One set of photos that share file size and pixel dimensions, plus a recommended keeper. */
data class DuplicateGroup(
    val photos: List<PhotoItem>,
    val suggestedKeepId: String,
) {
    /** Bytes that would be freed by keeping only the suggested photo. */
    val reclaimableBytes: Long
        get() = photos.asSequence()
            .filter { it.id != suggestedKeepId }
            .sumOf { it.sizeBytes }
}

/**
 * Metadata-only duplicate grouping with a keeper suggestion. Photos are matched by exact size
 * and dimensions, so nothing is decoded and no file is touched.
 */
object DuplicateFinder {

    /** Groups undecided photos; reviewed ones drop out so resolved groups disappear. */
    fun pendingGroups(items: List<PhotoWithState>): List<DuplicateGroup> =
        group(items.filterNot { it.state.isReviewed })

    /** Groups every photo regardless of state, for library-wide statistics. */
    fun allGroups(items: List<PhotoWithState>): List<DuplicateGroup> = group(items)

    private fun group(items: List<PhotoWithState>): List<DuplicateGroup> = items
        .filter {
            !it.photo.isVideo &&
                it.photo.sizeBytes > 0 &&
                it.photo.width > 0 &&
                it.photo.height > 0
        }
        .groupBy { SmartInsights.DuplicateKey(it.photo.sizeBytes, it.photo.width, it.photo.height) }
        .values
        .filter { it.size >= 2 }
        .sortedByDescending { it.size }
        .map { members -> DuplicateGroup(members.map { it.photo }, keeper(members)) }

    /**
     * Favourites win, then the newest capture. Size and dimensions are equal inside a group,
     * so the id tie-break only keeps the suggestion deterministic.
     */
    private fun keeper(members: List<PhotoWithState>): String = members
        .sortedWith(
            compareByDescending<PhotoWithState> { it.state.favorite }
                .thenByDescending { it.photo.capturedAt }
                .thenBy { it.photo.id },
        )
        .first()
        .photo
        .id
}
