package app.visto.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.visto.data.thumbnail.GeneratedThumbnailCache
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Displays a generated static thumbnail file instead of letting Coil persist
 * the original network response in its disk cache.
 *
 * Do not use this for animated images; generated WebP files are intentionally
 * static thumbnails.
 */
@Composable
fun GeneratedThumbnailImage(
    url: String,
    cacheKey: String,
    kind: GeneratedThumbnailCache.Kind,
    imageLoader: ImageLoader,
    contentDescription: String?,
    contentScale: ContentScale,
    cacheLimitBytes: Long,
    modifier: Modifier = Modifier,
    resumeDelayMs: Long = 0,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var request by remember(url, cacheKey, kind) { mutableStateOf<ImageRequest?>(null) }
    var failed by remember(url, cacheKey, kind) { mutableStateOf(false) }

    LaunchedEffect(url, cacheKey, kind, cacheLimitBytes) {
        request = null
        failed = false
        runCatching {
            GeneratedThumbnailCache.ensure(
                context = context.applicationContext,
                imageLoader = imageLoader,
                url = url,
                cacheKey = cacheKey,
                kind = kind,
                maxBytes = cacheLimitBytes,
            )
        }.onSuccess { file ->
            request = ImageRequest.Builder(context)
                .data(file)
                .memoryCacheKey("generated-thumb:$cacheKey:${kind.name}")
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .build()
        }.onFailure {
            failed = true
        }
    }

    val readyRequest = request
    if (readyRequest != null) {
        PausableAsyncImage(
            model = readyRequest,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            playAnimations = false,
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
