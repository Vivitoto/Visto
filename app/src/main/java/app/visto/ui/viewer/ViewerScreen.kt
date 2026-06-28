package app.visto.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.thumbnail.GeneratedThumbnailCache
import app.visto.ui.components.ANIMATED_THUMB_PREVIEW_PX
import app.visto.data.thumbnail.ThumbnailCacheKey
import app.visto.ui.components.AnimatedThumbnailImage
import app.visto.ui.components.GeneratedThumbnailImage
import app.visto.ui.layout.VistoLayoutMetrics
import app.visto.ui.layout.ViewerOverlayMetrics
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.OkHttpClient

/**
 * Visto media viewer.
 *
 * Images get pinch zoom and pan; videos are played with Media3/ExoPlayer.
 * The Authorization header is provided out-of-band by the shared OkHttp
 * client (used by both Coil and ExoPlayer), so the URL passed in does not
 * include credentials.
 *
 * When [autoLoadOriginalImages] is false, image pages display a small
 * translucent "加载原图" button over the image area. Tapping it loads the
 * full-resolution image. Once loaded for a page, it stays loaded for the
 * rest of the viewer session. Animated images also support loading the
 * original and follow the same auto-load / manual-load rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    session: ViewerSession,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    mediaUrlOf: (RemoteEntry) -> String,
    cacheKeyScope: String,
    thumbnailCacheLimitBytes: Long,
    autoLoadOriginalImages: Boolean,
    onClose: () -> Unit,
) {
    if (session.items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val pagerState = rememberPagerState(initialPage = session.initialIndex) { session.items.size }
    val manualLoaded = remember { mutableStateMapOf<String, Boolean>() }
    var chromeVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(pagerState.currentPage, autoLoadOriginalImages, session.items) {
        val currentPage = pagerState.currentPage
        val neighbors = ((currentPage - 2)..(currentPage + 2))
            .filter { it in session.items.indices && it != currentPage }
        neighbors.forEach { page ->
            val item = session.items[page]
            if (item.mediaType != MediaType.VIDEO) {
                val url = mediaUrlOf(item)
                val cacheKey = "$cacheKeyScope:${ThumbnailCacheKey.forEntry(item)}"
                if (item.mediaType == MediaType.ANIMATED_IMAGE) {
                    // Coil handles animated image caching natively — enqueue a preload.
                    runCatching {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .size(ANIMATED_THUMB_PREVIEW_PX)
                            .build()
                        imageLoader.enqueue(request)
                    }
                } else {
                    runCatching {
                        GeneratedThumbnailCache.ensure(
                            context = context.applicationContext,
                            imageLoader = imageLoader,
                            url = url,
                            cacheKey = cacheKey,
                            kind = GeneratedThumbnailCache.Kind.PREVIEW,
                            maxBytes = thumbnailCacheLimitBytes,
                        )
                    }
                }
                if (ViewerOriginalImagePolicy.shouldAutoLoadOriginal(item, autoLoadOriginalImages)) {
                    imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey("$cacheKeyScope:${item.path}:original:0")
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                    )
                }
            }
        }
    }

    Scaffold { innerPadding: PaddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
        ) {
            val overlayMetrics = VistoLayoutMetrics.viewerOverlay(maxWidth, maxHeight)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val item = session.items[page]
                when (item.mediaType) {
                    MediaType.VIDEO -> VideoPage(item, okHttpClient, mediaUrlOf(item))
                    else -> {
                        val loaded = ViewerOriginalImagePolicy.shouldShowOriginal(
                            item = item,
                            autoLoadOriginalImages = autoLoadOriginalImages,
                            manuallyLoaded = manualLoaded[item.path] == true,
                        )
                        ImagePage(
                            item = item,
                            imageLoader = imageLoader,
                            okHttpClient = okHttpClient,
                            url = mediaUrlOf(item),
                            cacheKeyScope = cacheKeyScope,
                            thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
                            loaded = loaded,
                            onLoadRequest = { manualLoaded[item.path] = true },
                            onToggleChrome = { chromeVisible = !chromeVisible },
                            overlayMetrics = overlayMetrics,
                        )
                    }
                }
            }
            if (chromeVisible) {
                val current = session.items.getOrNull(pagerState.currentPage)
                ViewerInfoBar(
                    current = current,
                    page = pagerState.currentPage + 1,
                    total = session.items.size,
                    onClose = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = overlayMetrics.edgePadding,
                            top = overlayMetrics.topPadding,
                            end = overlayMetrics.edgePadding,
                        ),
                    maxWidth = overlayMetrics.infoBarMaxWidth,
                )
            }
        }
    }
}

@Composable
private fun ViewerInfoBar(
    current: RemoteEntry?,
    page: Int,
    total: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.56f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.name.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = current?.let {
                        val type = when (it.mediaType) {
                            MediaType.VIDEO -> "VIDEO"
                            MediaType.ANIMATED_IMAGE -> "GIF"
                            else -> null
                        }
                        if (type == null) "$page / $total" else "$page / $total · $type"
                    }.orEmpty(),
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ImagePage(
    item: RemoteEntry,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    url: String,
    cacheKeyScope: String,
    thumbnailCacheLimitBytes: Long,
    loaded: Boolean,
    onLoadRequest: () -> Unit,
    onToggleChrome: () -> Unit,
    overlayMetrics: ViewerOverlayMetrics,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isZoomedIn by remember { mutableStateOf(false) }
    var loadAttempt by remember(item.path) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.path) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val canTransform = pressedCount >= 2 || isZoomedIn
                        if (canTransform) {
                            val zoom = if (pressedCount >= 2) event.calculateZoom() else 1f
                            val pan = if (isZoomedIn || pressedCount >= 2) {
                                event.calculatePan()
                            } else {
                                Offset.Zero
                            }
                            if (zoom != 1f || pan != Offset.Zero) {
                                val newScale = (scale * zoom).coerceIn(1f, 6f)
                                scale = newScale
                                isZoomedIn = newScale > 1.01f
                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(item.path) {
                // Double-tap remains a shortcut between fit (1x) and 2x.
                // One-finger horizontal swipes at 1x still pass through to
                // HorizontalPager; pinch zoom is handled above.
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = {
                        if (isZoomedIn) {
                            scale = 1f; offsetX = 0f; offsetY = 0f; isZoomedIn = false
                        } else {
                            scale = 2f; isZoomedIn = true
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (loaded) {
            val request = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .memoryCacheKey("$cacheKeyScope:${item.path}:original:$loadAttempt")
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build()
            SubcomposeAsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    ),
                loading = {
                    // While original is decoding, keep showing the cached
                    // thumbnail so the viewer never looks blank.
                    ThumbnailBackdrop(
                        item = item,
                        imageLoader = imageLoader,
                        okHttpClient = okHttpClient,
                        url = url,
                        cacheKeyScope = cacheKeyScope,
                        thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "正在加载原图…",
                            color = Color.White.copy(alpha = 0.70f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                },
                error = {
                    ThumbnailBackdrop(
                        item = item,
                        imageLoader = imageLoader,
                        okHttpClient = okHttpClient,
                        url = url,
                        cacheKeyScope = cacheKeyScope,
                        thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "原图加载失败",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = OriginalImageUiText.fileDetails(item.name, item.sizeBytes),
                            color = Color.White.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Button(
                            onClick = {
                                loadAttempt += 1
                                onLoadRequest()
                            },
                            modifier = Modifier.padding(top = 18.dp),
                        ) {
                            Text("重试")
                        }
                    }
                },
            )
        } else {
            // Default state when the user has not asked to load the original
            // yet: show the thumbnail as the backdrop and overlay a small
            // "load original" pill so the page is never blank. The backdrop
            // participates in the pinch-zoom transform so users can inspect
            // the thumbnail even before downloading the original.
            ThumbnailBackdrop(
                item = item,
                imageLoader = imageLoader,
                okHttpClient = okHttpClient,
                url = url,
                cacheKeyScope = cacheKeyScope,
                thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
            )
            if (ViewerOriginalImagePolicy.canLoadOriginal(item)) {
                LoadOriginalOverlay(item, onLoadRequest, overlayMetrics)
            }
        }
    }
}

@Composable
private fun ThumbnailBackdrop(
    item: RemoteEntry,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    url: String,
    cacheKeyScope: String,
    thumbnailCacheLimitBytes: Long,
    modifier: Modifier = Modifier,
) {
    val cacheKey = "$cacheKeyScope:${ThumbnailCacheKey.forEntry(item)}"
    if (item.mediaType == MediaType.ANIMATED_IMAGE) {
        AnimatedThumbnailImage(
            url = url,
            cacheKey = cacheKey,
            targetPx = ANIMATED_THUMB_PREVIEW_PX,
            imageLoader = imageLoader,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().then(modifier),
            loading = { /* blank — page just stays black briefly */ },
            error = { /* blank */ },
        )
    } else {
        GeneratedThumbnailImage(
            url = url,
            cacheKey = cacheKey,
            kind = GeneratedThumbnailCache.Kind.PREVIEW,
            imageLoader = imageLoader,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            cacheLimitBytes = thumbnailCacheLimitBytes,
            modifier = Modifier.fillMaxSize().then(modifier),
            loading = { /* blank — page just stays black briefly */ },
            error = { /* blank */ },
        )
    }
}

@Composable
private fun LoadOriginalOverlay(
    item: RemoteEntry,
    onLoad: () -> Unit,
    overlayMetrics: ViewerOverlayMetrics,
) {
    // Sits on top of the thumbnail backdrop. No solid background so the
    // thumbnail underneath stays visible; just a small pill at the bottom.
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            onClick = onLoad,
            shape = RoundedCornerShape(22.dp),
            color = Color.Black.copy(alpha = 0.55f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = overlayMetrics.edgePadding,
                    end = overlayMetrics.edgePadding,
                    bottom = overlayMetrics.bottomPadding,
                )
                .widthIn(max = overlayMetrics.actionMaxWidth),
        ) {
            Row(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = OriginalImageUiText.loadButtonLabel(item.sizeBytes),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VideoPage(item: RemoteEntry, okHttpClient: OkHttpClient, url: String) {
    val context = LocalContext.current
    val player = remember(item.path) {
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = false
            }
    }
    DisposableEffect(item.path) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
    )
}