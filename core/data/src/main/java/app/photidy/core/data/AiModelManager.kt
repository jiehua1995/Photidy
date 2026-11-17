package app.photidy.core.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** State of the optional on-device classification model. */
sealed interface AiModelState {
    data object NotDownloaded : AiModelState

    data class Downloading(val progress: Float) : AiModelState

    data object Ready : AiModelState

    data object Failed : AiModelState
}

/**
 * Downloads and stores the small MobileNet model used for optional local tagging.
 * The archive is intentionally tiny (about 3 MB) and is only fetched after the user
 * opts in; nothing about the photo library leaves the device.
 */
class AiModelManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val directory = File(context.filesDir, "ai")
    private val modelFile = File(directory, MODEL_FILE)
    private val labelsFile = File(directory, LABELS_FILE)
    private var job: Job? = null

    private val _state = MutableStateFlow<AiModelState>(
        if (isReady()) AiModelState.Ready else AiModelState.NotDownloaded,
    )
    val state: StateFlow<AiModelState> = _state.asStateFlow()

    fun isReady(): Boolean =
        modelFile.length() > MIN_MODEL_BYTES && labelsFile.length() > 0L

    fun modelFile(): File = modelFile

    fun labelsFile(): File = labelsFile

    fun download() {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = AiModelState.Downloading(0f)
            val tempDirectory = File(context.cacheDir, "ai-download").apply {
                deleteRecursively()
                mkdirs()
            }
            try {
                val archive = File(tempDirectory, "model.zip")
                downloadArchive(archive, isCancelled = { !isActive }) { progress ->
                    _state.value = AiModelState.Downloading(progress)
                }
                extract(archive, tempDirectory)
                directory.mkdirs()
                File(tempDirectory, MODEL_FILE).copyTo(modelFile, overwrite = true)
                File(tempDirectory, LABELS_FILE).copyTo(labelsFile, overwrite = true)
                _state.value = if (isReady()) AiModelState.Ready else AiModelState.Failed
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The URL, stack trace or response body must never end up in logs.
                _state.value = AiModelState.Failed
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
            _state.value = if (isReady()) AiModelState.Ready else AiModelState.NotDownloaded
        }
    }

    fun deleteModel() {
        cancelDownload()
        modelFile.delete()
        labelsFile.delete()
        _state.value = AiModelState.NotDownloaded
    }

    private fun downloadArchive(
        target: File,
        isCancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ) {
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
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

    private fun extract(archive: File, targetDirectory: File) {
        ZipInputStream(FileInputStream(archive)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && name in ARCHIVE_ENTRIES) {
                    File(targetDirectory, name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
    }

    companion object {
        /**
         * Official TensorFlow Lite sample archive. It bundles a quantized MobileNet V1
         * classifier (224x224, uint8) plus its ImageNet label list.
         */
        const val MODEL_URL =
            "https://storage.googleapis.com/download.tensorflow.org/models/tflite/" +
                "mobilenet_v1_1.0_224_quant_and_labels.zip"
        const val MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite"
        const val LABELS_FILE = "labels_mobilenet_quant_v1_224.txt"

        private val ARCHIVE_ENTRIES = setOf(MODEL_FILE, LABELS_FILE)

        // A truncated archive is far smaller than this; the real model is ~4.1 MB.
        private const val MIN_MODEL_BYTES = 3L * 1024 * 1024
    }
}
