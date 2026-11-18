package app.photidy.core.data

import android.content.Context
import app.photidy.core.model.AiModelTier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** State of the optional on-device classification model for the selected tier. */
sealed interface AiModelState {
    val tier: AiModelTier

    data class NotDownloaded(override val tier: AiModelTier) : AiModelState

    data class Downloading(override val tier: AiModelTier, val progress: Float) : AiModelState

    data class Ready(override val tier: AiModelTier) : AiModelState

    data class Failed(override val tier: AiModelTier) : AiModelState
}

/**
 * Downloads and stores one optional ImageNet classifier. Three sizes are available; only the
 * selected tier is kept on disk. Nothing is fetched until the user opts in, and the photo
 * library never leaves the device.
 */
class AiModelManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val directory = File(context.filesDir, "ai")
    private var job: Job? = null

    @Volatile
    private var tier: AiModelTier = AiModelTier.LIGHT

    private val _state = MutableStateFlow<AiModelState>(stateFor(AiModelTier.LIGHT))
    val state: StateFlow<AiModelState> = _state.asStateFlow()

    init {
        migrateLegacyModel()
        _state.value = stateFor(tier)
        scope.launch {
            settingsRepository.settings
                .map { it.aiModelTier }
                .distinctUntilChanged()
                .collect(::setTier)
        }
    }

    fun isReady(tier: AiModelTier = this.tier): Boolean {
        val file = AiModelSpecs.of(tier).fileFor(directory)
        return file.length() >= AiModelSpecs.of(tier).minBytes
    }

    fun modelFile(tier: AiModelTier = this.tier): File = AiModelSpecs.of(tier).fileFor(directory)

    fun currentTier(): AiModelTier = tier

    /** Switches tiers immediately; an in-flight download for the old tier is cancelled. */
    fun setTier(selected: AiModelTier) {
        if (selected == tier) return
        job?.cancel()
        job = null
        tier = selected
        _state.value = stateFor(selected)
    }

    fun download() {
        if (job?.isActive == true) return
        val targetTier = tier
        val spec = AiModelSpecs.of(targetTier)
        job = scope.launch {
            _state.value = AiModelState.Downloading(targetTier, 0f)
            val tempDirectory = File(context.cacheDir, "ai-download").apply {
                deleteRecursively()
                mkdirs()
            }
            try {
                val payload = File(tempDirectory, "download.bin")
                downloadFile(
                    url = spec.url,
                    target = payload,
                    isCancelled = { !isActive || tier != targetTier },
                ) { progress ->
                    if (tier == targetTier) {
                        _state.value = AiModelState.Downloading(targetTier, progress)
                    }
                }
                val model = if (spec.archiveEntry != null) {
                    extractEntry(payload, spec.archiveEntry, File(tempDirectory, "model.tflite"))
                } else {
                    payload
                }
                if (model.length() < spec.minBytes) error("Truncated model")
                directory.mkdirs()
                model.copyTo(spec.fileFor(directory), overwrite = true)
                // Keep a single model on disk so switching tiers never accumulates files.
                AiModelTier.entries
                    .filter { it != targetTier }
                    .forEach { AiModelSpecs.of(it).fileFor(directory).delete() }
                _state.value = when {
                    tier != targetTier -> stateFor(tier)
                    isReady(targetTier) -> AiModelState.Ready(targetTier)
                    else -> AiModelState.Failed(targetTier)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The URL, stack trace or response body must never end up in logs.
                if (tier == targetTier) _state.value = AiModelState.Failed(targetTier)
            } finally {
                tempDirectory.deleteRecursively()
            }
        }
    }

    /** Stops an in-flight download and clears the temporary files. */
    fun cancelDownload() {
        job?.cancel()
        job = null
        File(context.cacheDir, "ai-download").deleteRecursively()
        if (_state.value is AiModelState.Downloading) {
            _state.value = stateFor(tier)
        }
    }

    fun deleteModel() {
        cancelDownload()
        AiModelTier.entries.forEach { AiModelSpecs.of(it).fileFor(directory).delete() }
        _state.value = stateFor(tier)
    }

    private fun stateFor(tier: AiModelTier): AiModelState =
        if (isReady(tier)) AiModelState.Ready(tier) else AiModelState.NotDownloaded(tier)

    private fun downloadFile(
        url: String,
        target: File,
        isCancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) error("Unexpected response")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    var reported = 0f
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (isCancelled()) throw CancellationException("Model download cancelled")
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0L) {
                            val progress = (read.toFloat() / total).coerceIn(0f, 1f)
                            if (progress - reported >= 0.02f) {
                                reported = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractEntry(archive: File, entryName: String, target: File): File {
        ZipInputStream(FileInputStream(archive)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.substringAfterLast('/') == entryName) {
                    target.outputStream().use { output -> input.copyTo(output) }
                    return target
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        error("Model entry missing")
    }

    /**
     * Older builds stored a single MobileNet file plus a label list. Keep the model so
     * existing installs do not silently lose AI tagging after updating.
     */
    private fun migrateLegacyModel() {
        directory.mkdirs()
        val light = AiModelSpecs.of(AiModelTier.LIGHT)
        val legacyModel = File(directory, LEGACY_MODEL_FILE)
        val target = light.fileFor(directory)
        if (!target.exists() && legacyModel.length() >= light.minBytes) {
            runCatching { legacyModel.copyTo(target, overwrite = true) }
        }
        legacyModel.delete()
        File(directory, LEGACY_LABELS_FILE).delete()
    }

    private companion object {
        const val LEGACY_MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite"
        const val LEGACY_LABELS_FILE = "labels_mobilenet_quant_v1_224.txt"
    }
}

internal data class AiModelSpec(
    val tier: AiModelTier,
    val url: String,
    /** Zip entry to extract, or null when [url] points straight at a .tflite file. */
    val archiveEntry: String?,
    val fileName: String,
    val minBytes: Long,
) {
    fun fileFor(directory: File): File = File(directory, fileName)
}

internal object AiModelSpecs {

    private const val MOBILENET_V1_ARCHIVE =
        "https://storage.googleapis.com/download.tensorflow.org/models/tflite/" +
            "mobilenet_v1_1.0_224_quant_and_labels.zip"
    private const val EFFICIENTNET_LITE0 =
        "https://storage.googleapis.com/mediapipe-models/image_classifier/" +
            "efficientnet_lite0/int8/1/efficientnet_lite0.tflite"
    private const val EFFICIENTNET_LITE2 =
        "https://storage.googleapis.com/mediapipe-models/image_classifier/" +
            "efficientnet_lite2/int8/1/efficientnet_lite2.tflite"

    private val specs: Map<AiModelTier, AiModelSpec> = listOf(
        AiModelSpec(
            tier = AiModelTier.LIGHT,
            url = MOBILENET_V1_ARCHIVE,
            archiveEntry = "mobilenet_v1_1.0_224_quant.tflite",
            fileName = "model_light.tflite",
            minBytes = 3L * 1024 * 1024,
        ),
        AiModelSpec(
            tier = AiModelTier.BALANCED,
            url = EFFICIENTNET_LITE0,
            archiveEntry = null,
            fileName = "model_balanced.tflite",
            minBytes = 4L * 1024 * 1024,
        ),
        AiModelSpec(
            tier = AiModelTier.ACCURATE,
            url = EFFICIENTNET_LITE2,
            archiveEntry = null,
            fileName = "model_accurate.tflite",
            minBytes = 6L * 1024 * 1024,
        ),
    ).associateBy { it.tier }

    fun of(tier: AiModelTier): AiModelSpec = specs.getValue(tier)
}
