package app.photidy.core.data

import app.photidy.core.model.AiModelTier
import app.photidy.core.model.AiTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

data class AiScanProgress(
    val running: Boolean = false,
    val done: Int = 0,
    val remaining: Int = 0,
) {
    val total: Int
        get() = done + remaining
}

/**
 * Labels the whole library once the user opted in, then keeps labeling only newly seen photos.
 * Photos without a content URI (demo artwork) are ignored, and a photo is remembered as analyzed
 * even when no label matched. Upgrading the model tier re-labels everything with the new model.
 */
class AiAnalysisCoordinator(
    private val photoRepository: PhotoRepository,
    private val settingsRepository: SettingsRepository,
    private val aiModelManager: AiModelManager,
    private val aiTagger: AiTagger,
    private val scope: CoroutineScope,
) {

    private val _progress = MutableStateFlow(AiScanProgress())
    val progress: StateFlow<AiScanProgress> = _progress.asStateFlow()

    private var job: Job? = null
    private var activeTier: AiModelTier? = null

    init {
        scope.launch {
            combine(
                settingsRepository.settings,
                aiModelManager.state,
            ) { settings, model ->
                if (settings.aiRecognitionEnabled && model is AiModelState.Ready) model.tier else null
            }
                .distinctUntilChanged()
                .collect { tier -> if (tier != null) start(tier) else stop() }
        }
    }

    private fun start(tier: AiModelTier) {
        if (job?.isActive == true && activeTier == tier) return
        job?.cancel()
        activeTier = tier
        job = scope.launch {
            photoRepository.photos.collect { photos ->
                if (!aiModelManager.isReady(tier)) {
                    _progress.value = AiScanProgress()
                    return@collect
                }

                // Tags from another model tier are stale; drop them before re-labelling.
                val stale = photos
                    .filter { it.state.aiAnalyzed && it.state.aiModelTier != tier }
                    .mapTo(HashSet()) { it.photo.id }
                if (stale.isNotEmpty()) {
                    _progress.value = AiScanProgress()
                    photoRepository.resetAiTags(stale)
                    return@collect
                }

                val pending = photos.filter {
                    it.photo.contentUri != null &&
                        !it.photo.isVideo &&
                        (!it.state.aiAnalyzed || it.state.aiModelTier != tier)
                }
                if (pending.isEmpty()) {
                    _progress.value = AiScanProgress()
                    return@collect
                }

                _progress.value = AiScanProgress(running = true, remaining = pending.size)
                var done = 0
                for (batch in pending.chunked(BATCH_SIZE)) {
                    currentCoroutineContext().ensureActive()
                    if (!aiModelManager.isReady(tier)) return@collect

                    val analyzedIds = HashSet<String>(batch.size)
                    val tags = HashMap<String, List<AiTag>>()
                    batch.forEach { item ->
                        val result = aiTagger.analyze(item.photo)
                        analyzedIds += item.photo.id
                        if (result.isNotEmpty()) tags[item.photo.id] = result
                    }
                    if (!aiModelManager.isReady(tier)) return@collect

                    photoRepository.saveAiTags(analyzedIds, tags, tier)
                    done += batch.size
                    _progress.value = AiScanProgress(
                        running = true,
                        done = done,
                        remaining = (pending.size - done).coerceAtLeast(0),
                    )
                    // Let the UI and other collectors run between batches.
                    yield()
                }
                _progress.value = AiScanProgress()
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
        activeTier = null
        _progress.value = AiScanProgress()
    }

    private companion object {
        const val BATCH_SIZE = 8
    }
}
