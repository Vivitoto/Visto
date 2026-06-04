package app.visto.data.thumbnail

import android.content.Context
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient

/**
 * Builds the [ImageLoader] Visto uses for grid thumbnails and the viewer.
 *
 * Notes:
 *  - Disk cache: bounded so Coil's own cache doubles as the visible-area
 *    LRU for thumbnails. 200 MB is conservative for v0.1 and additive with
 *    [ThumbnailLruPolicy] cleanup.
 *  - Animated decoders are wired in so animated WebP and GIF still render
 *    statically (first frame) inside grid contexts. The viewer can opt in
 *    to animated playback in Phase 7.
 *  - Video frames are extracted via [VideoFrameDecoder].
 *  - Authenticated GET is supplied by the caller's [okHttpClient]; the
 *    Authorization header is computed per-request in [app.visto.data.webdav.WebDavClient].
 */
object VistoImageLoaderFactory {

    fun create(
        context: Context,
        okHttpClient: OkHttpClient,
        diskCacheBytes: Long = 500L * 1024 * 1024,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.10)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("visto_thumbs"))
                .maxSizeBytes(diskCacheBytes)
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .components {
            add(VideoFrameDecoder.Factory())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
