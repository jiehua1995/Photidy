package app.photidy.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.photidy.core.model.AiTag
import app.photidy.core.model.AiTagging
import app.photidy.core.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the selected on-device ImageNet classifier on a single photo. Everything happens on
 * device: the bitmap is decoded into the model's input size, one forward pass is executed,
 * and only the coarse [AiTag] result is kept. Input and output tensor types are read from
 * the model, so the three downloadable tiers share one code path.
 */
class AiTagger(
    private val context: Context,
    private val modelManager: AiModelManager,
) {

    private val mutex = Mutex()

    @Volatile
    private var interpreter: Interpreter? = null

    @Volatile
    private var labels: List<String> = emptyList()

    @Volatile
    private var loadedModelKey: String = ""

    @Volatile
    private var inputSize: Int = FALLBACK_INPUT_SIZE

    suspend fun analyze(photo: PhotoItem): List<AiTag> = withContext(Dispatchers.Default) {
        val uri = photo.contentUri ?: return@withContext emptyList()
        if (!modelManager.isReady()) return@withContext emptyList()
        val ready = ensureInterpreter() ?: return@withContext emptyList()
        val bitmap = decode(uri, inputSize) ?: return@withContext emptyList()
        try {
            runCatching { classify(ready, bitmap) }.getOrElse { emptyList() }
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun ensureInterpreter(): Interpreter? = mutex.withLock {
        val file = modelManager.modelFile()
        val key = "${file.path}:${file.length()}"
        interpreter?.let { loaded -> if (loadedModelKey == key) return loaded }
        if (!modelManager.isReady()) return null

        runCatching {
            interpreter?.close()
            val loaded = Interpreter(file, Interpreter.Options().apply { setNumThreads(2) })
            val shape = loaded.getInputTensor(0).shape()
            inputSize = if (shape.size == 4 && shape[3] == 3) shape[1] else FALLBACK_INPUT_SIZE
            labels = readLabels()
            interpreter = loaded
            loadedModelKey = key
            loaded
        }.getOrNull()
    }

    private fun readLabels(): List<String> =
        context.assets.open(LABELS_ASSET).bufferedReader().use { reader ->
            reader.readLines().map(String::trim).filter(String::isNotEmpty)
        }

    private fun classify(interpreter: Interpreter, bitmap: Bitmap): List<AiTag> {
        val size = inputSize
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        return try {
            val pixels = IntArray(size * size)
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)

            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            val inputType = inputTensor.dataType()
            val outputType = outputTensor.dataType()
            if (inputType != DataType.UINT8 && inputType != DataType.INT8) return emptyList()
            if (outputType != DataType.UINT8 && outputType != DataType.INT8) return emptyList()
            val classCount = outputTensor.shape().last()

            val input = byteInput(pixels, inputType)
            val output = Array(1) { ByteArray(classCount) }
            interpreter.run(input, output)
            val isSigned = outputType == DataType.INT8
            topTags(output[0].map { byte ->
                val value = if (isSigned) byte.toInt() + 128 else byte.toInt() and 0xFF
                value.toDouble() / 255.0
            })
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun byteInput(pixels: IntArray, type: DataType): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(pixels.size * 3).apply {
            order(ByteOrder.nativeOrder())
        }
        val signed = type == DataType.INT8
        pixels.forEach { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            buffer.put(if (signed) (red - 128).toByte() else red.toByte())
            buffer.put(if (signed) (green - 128).toByte() else green.toByte())
            buffer.put(if (signed) (blue - 128).toByte() else blue.toByte())
        }
        buffer.rewind()
        return buffer
    }

    private fun topTags(scores: List<Double>): List<AiTag> {
        val labelList = labels
        if (labelList.isEmpty() || scores.isEmpty()) return emptyList()
        val topLabels = scores.withIndex()
            .sortedByDescending { it.value }
            .take(TOP_K)
            .filter { it.value >= MIN_SCORE }
            .mapNotNull { (index, _) ->
                // Some ImageNet classifiers add a background class at index 0.
                val shifted = if (scores.size == labelList.size + 1) index - 1 else index
                labelList.getOrNull(shifted)
            }
        return AiTagging.tagsFor(topLabels)
    }

    private fun decode(uriString: String, targetSize: Int): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / targetSize)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val cropped = centerCrop(bitmap)
        if (cropped !== bitmap) bitmap.recycle()
        cropped
    }.getOrNull()

    private fun centerCrop(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        if (size <= 0) return bitmap
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, left, top, size, size)
    }

    private companion object {
        const val LABELS_ASSET = "imagenet_labels.txt"
        const val FALLBACK_INPUT_SIZE = 224
        const val TOP_K = 5

        /** Quantized scores are 0..255; ~25% keeps weak guesses out. */
        const val MIN_SCORE = 0.25
    }
}
