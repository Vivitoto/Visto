package app.visto.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.net.Uri
import android.os.Build
import com.aureusapps.android.webpandroid.decoder.WebPDecoder
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Re-encodes animated GIF/WebP sources into small animated WebP thumbnails.
 *
 * The source file is downloaded to a temporary cache file only for transcoding;
 * it is deleted immediately after the thumbnail is produced. This preserves
 * animation in grids without keeping the original multi-megabyte animation in
 * Coil's disk cache.
 */
object AnimatedThumbnailCache {
    enum class Kind(
        val targetPx: Int,
        val quality: Float,
        val frameStepMs: Int,
        val maxFrames: Int,
    ) {
        GRID(targetPx = 320, quality = 68f, frameStepMs = 100, maxFrames = 10),
        PREVIEW(targetPx = 1024, quality = 85f, frameStepMs = 83, maxFrames = 60),
    }

    private const val DIR_NAME = "visto_generated_animated_thumbs"
    private const val SOURCE_SUFFIX = ".source"

    /** Dedicated single thread — same-thread JNI affinity prevents thread-local native crashes. */
    private val webpDispatcher = newSingleThreadContext("webp-jni")

    private enum class SourceKind { GIF, WEBP }

    private data class ThumbFrame(
        val timestampMs: Long,
        val bitmap: Bitmap,
    )

    fun sizeBytes(context: Context): Long = cacheDir(context)
        .walkTopDown()
        .filter { it.isFile && it.extension == "webp" }
        .sumOf { it.length() }

    fun clear(context: Context) {
        cacheDir(context).deleteRecursively()
    }

    suspend fun ensure(
        context: Context,
        okHttpClient: OkHttpClient,
        url: String,
        cacheKey: String,
        kind: Kind,
        maxBytes: Long,
    ): File = withContext(webpDispatcher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            error("Animated WebP thumbnails require Android 9+ playback support")
        }
        val dir = cacheDir(context).also { it.mkdirs() }
        val file = File(dir, "${hashedName(cacheKey, kind)}.webp")
        if (file.isFile && file.length() > 0L) {
            file.setLastModified(System.currentTimeMillis())
            return@withContext file
        }

        val source = File(dir, "${file.name}$SOURCE_SUFFIX")
        val tmp = File(dir, "${file.name}.tmp")
        var frame: ThumbFrame? = null
        try {
            downloadSource(okHttpClient, url, source)
            frame = decodeFirstFrame(context, source, kind)
            encodeFirstFrame(frame.bitmap, kind.quality, tmp)
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            file.setLastModified(System.currentTimeMillis())
            evictOldestIfNeeded(dir, maxBytes)
            file
        } finally {
            frame?.bitmap?.recycle()
            source.delete()
            tmp.delete()
        }
    }

    private fun downloadSource(okHttpClient: OkHttpClient, url: String, target: File) {
        val client = okHttpClient.newBuilder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unable to fetch animated thumbnail source: HTTP ${response.code}")
            val body = response.body ?: error("Animated thumbnail source has no response body")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }

    // ── First-frame decoding ──────────────────────────────────────────

    private fun decodeFirstFrame(context: Context, source: File, kind: Kind): ThumbFrame {
        return when (source.detectKind()) {
            SourceKind.WEBP -> decodeWebpFirstFrame(context, source, kind)
            SourceKind.GIF -> decodeGifFirstFrame(source, kind)
        }
    }

    private fun decodeWebpFirstFrame(context: Context, source: File, kind: Kind): ThumbFrame {
        val decoder = WebPDecoder(context)
        return try {
            decoder.setDataSource(Uri.fromFile(source))
            val info = decoder.decodeInfo()
            val target = targetSize(info.width, info.height, kind.targetPx)
            if (decoder.hasNextFrame()) {
                val result = decoder.decodeNextFrame()
                val frame = result.frame ?: error("WebP first frame is null")
                ThumbFrame(0L, frame.scaleTo(target.width, target.height))
            } else {
                error("WebP source has no frames")
            }
        } finally {
            decoder.release()
        }
    }

    private fun decodeGifFirstFrame(source: File, kind: Kind): ThumbFrame {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(source)
            val bitmap = ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.setTargetSize(kind.targetPx, kind.targetPx)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val target = targetSize(bitmap.width, bitmap.height, kind.targetPx)
            val scaled = if (bitmap.width == target.width && bitmap.height == target.height) bitmap
            else Bitmap.createScaledBitmap(bitmap, target.width, target.height, true)
            if (scaled !== bitmap) bitmap.recycle()
            return ThumbFrame(0L, scaled)
        }
        @Suppress("DEPRECATION")
        val movie = source.inputStream().use { Movie.decodeStream(it) }
            ?: error("Unable to decode GIF source")
        val sourceWidth = movie.width().coerceAtLeast(1)
        val sourceHeight = movie.height().coerceAtLeast(1)
        val target = targetSize(sourceWidth, sourceHeight, kind.targetPx)
        val bitmap = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = minOf(
            target.width.toFloat() / sourceWidth.toFloat(),
            target.height.toFloat() / sourceHeight.toFloat(),
        )
        canvas.scale(scale, scale)
        movie.setTime(0)
        movie.draw(canvas, 0f, 0f)
        return ThumbFrame(0L, bitmap)
    }

    private fun encodeFirstFrame(bitmap: Bitmap, quality: Float, target: File) {
        val q = (quality.coerceIn(0f, 100f)).roundToInt()
        target.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.WEBP, q, out)) {
                error("Failed to compress first frame as WebP")
            }
        }
    }

    private fun targetSize(width: Int, height: Int, targetPx: Int): Size {
        val longest = maxOf(width, height).coerceAtLeast(1)
        if (longest <= targetPx) return Size(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val scale = targetPx.toFloat() / longest.toFloat()
        return Size(
            width = (width * scale).roundToInt().coerceAtLeast(1),
            height = (height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    private fun Bitmap.scaleTo(width: Int, height: Int): Bitmap {
        val source = if (config == Bitmap.Config.ARGB_8888 && !isRecycled) this else copy(Bitmap.Config.ARGB_8888, false)
        if (source.width == width && source.height == height) return source.copy(Bitmap.Config.ARGB_8888, false)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun File.detectKind(): SourceKind {
        val header = inputStream().use { input -> ByteArray(16).also { input.read(it) } }
        return when {
            header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte() -> SourceKind.GIF
            header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() && header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte() -> SourceKind.WEBP
            else -> error("Unsupported animated thumbnail source")
        }
    }

    private fun cacheDir(context: Context): File = File(context.cacheDir, DIR_NAME)

    private fun hashedName(cacheKey: String, kind: Kind): String {
        val source = "animated|${kind.name}|$cacheKey"
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun evictOldestIfNeeded(dir: File, maxBytes: Long) {
        if (maxBytes <= 0L) return
        val files = dir.walkTopDown().filter { it.isFile && it.extension == "webp" }.toList()
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= maxBytes) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private data class Size(val width: Int, val height: Int)
}