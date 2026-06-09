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
import app.visto.data.thumbnail.AnimatedThumbnailCache
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.OkHttpClient

/**
 * Displays a small generated animated WebP thumbnail. If transcoding fails,
 * falls back to the original animated URL with Coil disk cache disabled so the
 * original animation is not persisted as the thumbnail cache entry.
 */
@Composable
fun AnimatedThumbnailImage(
    url: String,
    cacheKey: String,
    kind: AnimatedThumbnailCache.Kind,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    contentDescription: String?,
    contentScale: ContentScale,
    cacheLimitBytes: Long,
    modifier: Modifier = Modifier,
    playAnimations: Boolean = true,
    resumeDelayMs: Long = 0,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var request by remember(url, cacheKey, kind) { mutableStateOf<ImageRequest?>(null) }
    var useOriginalFallback by remember(url, cacheKey, kind) { mutableStateOf(false) }

    LaunchedEffect(url, cacheKey, kind, cacheLimitBytes) {
        request = null
        useOriginalFallback = false
        runCatching {
            AnimatedThumbnailCache.ensure(
                context = context.applicationContext,
                okHttpClient = okHttpClient,
                url = url,
                cacheKey = cacheKey,
                kind = kind,
                maxBytes = cacheLimitBytes,
            )
        }.onSuccess { file ->
            request = ImageRequest.Builder(context)
                .data(file)
                .memoryCacheKey("generated-animated-thumb:${kind.name}:$cacheKey")
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .build()
        }.onFailure {
            useOriginalFallback = true
            request = ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey("animated-original-fallback:${kind.name}:$cacheKey")
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .build()
        }
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
        Box(modifier = modifier) { loading() }
    }
}
