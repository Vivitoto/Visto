package app.visto.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/** Target size for animated thumbnails in album grids. */
const val ANIMATED_THUMB_GRID_PX = 320

/** Target size for animated thumbnails in the viewer backdrop. */
const val ANIMATED_THUMB_PREVIEW_PX = 1024

/**
 * Loads an animated image (GIF / WebP) at a controlled decode size via Coil.
 * No custom re-encoding — uses Coil's platform-native animated image support,
 * with disk cache enabled so originals are cached and not re-downloaded on restart.
 * That matches the v0.1.8 stable behaviour for animated thumbnails.
 */
@Composable
fun AnimatedThumbnailImage(
    url: String,
    cacheKey: String,
    targetPx: Int,
    imageLoader: ImageLoader,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    playAnimations: Boolean = true,
    resumeDelayMs: Long = 0,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var request by remember(url, cacheKey, targetPx) {
        mutableStateOf<ImageRequest?>(null)
    }
    var failed by remember(url, cacheKey, targetPx) {
        mutableStateOf(false)
    }

    if (request == null && !failed) {
        // Build the Coil request synchronously on composition.
        // Coil handles animated GIF/WebP natively; .size() sets the decode
        // resolution so we don't keep the original multi-megapixel frames
        // in memory.
        val req = ImageRequest.Builder(context)
            .data(url)
            .size(targetPx)
            .memoryCacheKey("animated-thumb:$cacheKey:$targetPx")
            .crossfade(false)
            .build()
        request = req
    }

    val readyRequest = request
    if (readyRequest != null) {
        PausableAsyncImage(
            model = readyRequest,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            playAnimations = playAnimations,
            resumeDelayMs = resumeDelayMs,
            modifier = modifier,
            loading = loading,
            error = error,
        )
    } else {
        Box(modifier = modifier) {
            if (failed) error() else loading()
        }
    }
}