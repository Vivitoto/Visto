package app.visto.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Small, re-encoded on-disk thumbnails for images.
 *
 * Coil's normal disk cache stores the network source response, which can be a
 * multi-megabyte original photo even when the UI only asks to display a 320px
 * thumbnail. This cache stores only downsampled static WebP thumbnails and
 * disables Coil disk caching while generating them, so image browsing does not
 * fill disk with originals.
 *
 * Animated GIF / WebP sources are intentionally represented by a static
 * decoded frame here. That keeps album grids and viewer previews away from
 * multi-frame thumbnail transcoding.
 */
object GeneratedThumbnailCache {
    enum class Kind(val targetPx: Int, val quality: Int) {
        GRID(targetPx = 320, quality = 80),
        PREVIEW(targetPx = 1024, quality = 85),
    }

    private const val DIR_NAME = "visto_generated_thumbs"

    fun sizeBytes(context: Context): Long = cacheDir(context)
        .walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }

    fun clear(context: Context) {
        cacheDir(context).deleteRecursively()
    }

    suspend fun ensure(
        context: Context,
        imageLoader: ImageLoader,
        url: String,
        cacheKey: String,
        kind: Kind,
        maxBytes: Long,
    ): File = withContext(Dispatchers.IO) {
        val dir = cacheDir(context).also { it.mkdirs() }
        val file = File(dir, "${hashedName(cacheKey, kind)}.webp")
        if (file.isFile && file.length() > 0L) {
            file.setLastModified(System.currentTimeMillis())
            return@withContext file
        }

        val tmp = File(dir, "${file.name}.tmp")
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .memoryCacheKey("generated-source:$cacheKey:${kind.name}")
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(kind.targetPx)
            .precision(Precision.INEXACT)
            .build()
        val result = imageLoader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable
            ?: error("Unable to decode thumbnail source")
        val bitmap = drawable.toBitmap()
        val scaled = bitmap.scaleDownToMaxSide(kind.targetPx)
        tmp.outputStream().use { out ->
            @Suppress("DEPRECATION")
            scaled.compress(Bitmap.CompressFormat.WEBP, kind.quality, out)
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        file.setLastModified(System.currentTimeMillis())
        evictOldestIfNeeded(dir, maxBytes)
        file
    }

    private fun cacheDir(context: Context): File = File(context.cacheDir, DIR_NAME)

    private fun hashedName(cacheKey: String, kind: Kind): String {
        val source = "${kind.name}|$cacheKey"
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun Bitmap.scaleDownToMaxSide(maxSide: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val scale = maxSide.toFloat() / longest.toFloat()
        val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
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
}
