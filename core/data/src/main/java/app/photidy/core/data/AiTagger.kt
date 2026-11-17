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
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the optional quantized MobileNet classifier on a single photo. Everything happens
 * on device: the bitmap is decoded to a 224x224 buffer, one forward pass is executed, and
 * only the coarse [AiTag] result is kept.
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
    private var loadedModelLength: Long = -1L

    suspend fun analyze(photo: PhotoItem): List<AiTag> = withContext(Dispatchers.Default) {
        val uri = photo.contentUri ?: return@withContext emptyList()
        if (!modelManager.isReady()) return@withContext emptyList()
        val bitmap = decode(uri) ?: return@withContext emptyList()
        try {
            val ready = ensureInterpreter() ?: return@withContext emptyList()
            runCatching { classify(ready, bitmap) }.getOrElse { emptyList() }
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun ensureInterpreter(): Interpreter? = mutex.withLock {
        val length = modelManager.modelFile().length()
        interpreter?.let { loaded -> if (loadedModelLength == length) return loaded }
        if (!modelManager.isReady()) return null

        runCatching {
            interpreter?.close()
            val options = Interpreter.Options().apply { setNumThreads(2) }
            val loaded = Interpreter(modelManager.modelFile(), options)
            labels = modelManager.labelsFile().readLines().map(String::trim)
            interpreter = loaded
            loadedModelLength = length
            loaded
        }.getOrNull()
    }

    private fun classify(interpreter: Interpreter, bitmap: Bitmap): List<AiTag> {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        return try {
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            val input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3).apply {
                order(ByteOrder.nativeOrder())
                pixels.forEach { pixel ->
                    put(((pixel shr 16) and 0xFF).toByte())
                    put(((pixel shr 8) and 0xFF).toByte())
                    put((pixel and 0xFF).toByte())
                }
                rewind()
            }

            val classCount = interpreter.getOutputTensor(0).shape().last()
            val output = Array(1) { ByteArray(classCount) }
            interpreter.run(input, output)

            val topLabels = output[0].withIndex()
                .sortedByDescending { it.value.toInt() and 0xFF }
                .take(TOP_K)
                .filter { (it.value.toInt() and 0xFF) >= MIN_SCORE }
                .mapNotNull { (index, _) -> labels.getOrNull(index) }
            AiTagging.tagsFor(topLabels)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun decode(uriString: String): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / INPUT_SIZE)
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
        const val INPUT_SIZE = 224
        const val TOP_K = 5

        /** Quantized scores are 0..255; ~25% keeps weak guesses out. */
        const val MIN_SCORE = 64
    }
}
