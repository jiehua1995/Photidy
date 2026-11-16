package app.photidy

import android.app.Application
import app.photidy.core.data.PhotidyGraph
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class PhotidyApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        PhotidyGraph.init(this)
    }

    /**
     * Shared loader for real media. The video decoder only runs for video content URIs;
     * image decoding stays on Coil's default background dispatcher.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(160)
        .build()
}
