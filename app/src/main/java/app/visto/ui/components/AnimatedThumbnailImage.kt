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
import app.visto.data.thumbnail.GeneratedThumbnailCache
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.OkHttpClient

/**
 * Displays a small generated animated WebP thumbnail. If transcoding fails,
 * falls back to a static thumbnail via [GeneratedThumbnailCache] instead of
 * loading the original animated source, to keep memory predictable.
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
    allowOriginalFallback: Boolean = true,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var request by remember(url, cacheKey, kind, allowOriginalFallback) {
        mutableStateOf<ImageRequest?>(null)
    }
    var failed by remember(url, cacheKey, kind, allowOriginalFallback) {
        mutableStateOf(false)
    }

    LaunchedEffect(url, cacheKey, kind, cacheLimitBytes, allowOriginalFallback) {
        request = null
        failed = false
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
            if (allowOriginalFallback) {
                runCatching {
                    GeneratedThumbnailCache.ensure(
                        context = context.applicationContext,
                        imageLoader = imageLoader,
                        url = url,
                        cacheKey = cacheKey,
                        kind = when (kind) {
                            AnimatedThumbnailCache.Kind.GRID -> GeneratedThumbnailCache.Kind.GRID
                            AnimatedThumbnailCache.Kind.PREVIEW -> GeneratedThumbnailCache.Kind.PREVIEW
                        },
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
            } else {
                failed = true
            }
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
        Box(modifier = modifier) {
            if (failed) error() else loading()
        }
    }
}
