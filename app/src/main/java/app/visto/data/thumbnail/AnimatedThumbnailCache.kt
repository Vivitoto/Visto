package app.visto.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri
import android.os.Build
import com.aureusapps.android.webpandroid.decoder.WebPDecoder
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoderOptions
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import com.aureusapps.android.webpandroid.encoder.WebPMuxAnimParams
import com.aureusapps.android.webpandroid.encoder.WebPPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
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
        GRID(targetPx = 320, quality = 68f, frameStepMs = 100, maxFrames = 120),
        PREVIEW(targetPx = 768, quality = 78f, frameStepMs = 83, maxFrames = 180),
    }

    private const val DIR_NAME = "visto_generated_animated_thumbs"
    private const val SOURCE_SUFFIX = ".source"

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
    ): File = withContext(Dispatchers.IO) {
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
        var frames: List<ThumbFrame> = emptyList()
        try {
            downloadSource(okHttpClient, url, source)
            frames = when (source.detectKind()) {
                SourceKind.WEBP -> decodeAnimatedWebpFrames(context, source, kind)
                SourceKind.GIF -> sampleGifFrames(source, kind)
            }
            encodeAnimatedWebp(context, frames, kind, tmp)
            if (!tmp.isFile || tmp.length() <= 0L) error("Animated thumbnail output is empty")
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            file.setLastModified(System.currentTimeMillis())
            evictOldestIfNeeded(dir, maxBytes)
            file
        } finally {
            frames.forEach { it.bitmap.recycle() }
            source.delete()
            tmp.delete()
        }
    }

    private fun downloadSource(okHttpClient: OkHttpClient, url: String, target: File) {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unable to fetch animated thumbnail source: HTTP ${response.code}")
            val body = response.body ?: error("Animated thumbnail source has no response body")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }

    private fun decodeAnimatedWebpFrames(context: Context, source: File, kind: Kind): List<ThumbFrame> {
        val decoder = WebPDecoder(context)
        return try {
            decoder.setDataSource(Uri.fromFile(source))
            val info = decoder.decodeInfo()
            if (!info.hasAnimation) error("WebP source is not animated")

            val target = targetSize(info.width, info.height, kind.targetPx)
            val frames = mutableListOf<ThumbFrame>()
            var lastAcceptedTimestamp = Long.MIN_VALUE
            while (decoder.hasNextFrame() && frames.size < kind.maxFrames) {
                val result = decoder.decodeNextFrame()
                val frame = result.frame ?: break
                val timestamp = result.timestamp.toLong().coerceAtLeast(0L)
                if (frames.isEmpty() || timestamp - lastAcceptedTimestamp >= kind.frameStepMs) {
                    frames += ThumbFrame(
                        timestampMs = normalizedTimestamp(timestamp, frames),
                        bitmap = frame.scaleTo(target.width, target.height),
                    )
                    lastAcceptedTimestamp = timestamp
                }
            }
            if (frames.isEmpty()) error("Animated WebP source produced no frames")
            frames
        } finally {
            decoder.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun sampleGifFrames(source: File, kind: Kind): List<ThumbFrame> {
        val movie = source.inputStream().use { Movie.decodeStream(it) }
            ?: error("Unable to decode GIF source")
        val sourceWidth = movie.width().coerceAtLeast(1)
        val sourceHeight = movie.height().coerceAtLeast(1)
        val target = targetSize(sourceWidth, sourceHeight, kind.targetPx)
        val duration = movie.duration().takeIf { it > 0 } ?: 1000
        val frames = mutableListOf<ThumbFrame>()
        var t = 0
        while (t < duration && frames.size < kind.maxFrames) {
            movie.setTime(t)
            val bitmap = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = minOf(
                target.width.toFloat() / sourceWidth.toFloat(),
                target.height.toFloat() / sourceHeight.toFloat(),
            )
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
            frames += ThumbFrame(timestampMs = normalizedTimestamp(t.toLong(), frames), bitmap = bitmap)
            t += kind.frameStepMs
        }
        if (frames.isEmpty()) error("GIF source produced no frames")
        return frames
    }

    private fun encodeAnimatedWebp(context: Context, frames: List<ThumbFrame>, kind: Kind, target: File) {
        require(frames.isNotEmpty())
        val first = frames.first().bitmap
        val encoder = WebPAnimEncoder(
            context = context,
            width = first.width,
            height = first.height,
            options = WebPAnimEncoderOptions(
                minimizeSize = true,
                animParams = WebPMuxAnimParams(
                    backgroundColor = 0x00000000,
                    loopCount = 0,
                ),
            ),
        )
        try {
            encoder.configure(
                config = WebPConfig(
                    lossless = WebPConfig.COMPRESSION_LOSSY,
                    quality = kind.quality,
                    method = 4,
                    alphaQuality = 80,
                    threadLevel = 1,
                    lowMemory = true,
                ),
                preset = WebPPreset.WEBP_PRESET_PICTURE,
            )
            frames.forEach { frame -> encoder.addFrame(frame.timestampMs, frame.bitmap) }
            encoder.assemble(animationEndTimestamp(frames, kind), Uri.fromFile(target))
        } finally {
            encoder.release()
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

    private fun normalizedTimestamp(timestamp: Long, existing: List<ThumbFrame>): Long {
        val previous = existing.lastOrNull()?.timestampMs ?: return timestamp.coerceAtLeast(0L)
        return timestamp.coerceAtLeast(previous + 1L)
    }

    private fun animationEndTimestamp(frames: List<ThumbFrame>, kind: Kind): Long {
        val last = frames.last().timestampMs
        val previous = frames.dropLast(1).lastOrNull()?.timestampMs
        val frameDuration = previous?.let { (last - it).coerceAtLeast(1L) } ?: kind.frameStepMs.toLong()
        return last + frameDuration
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
