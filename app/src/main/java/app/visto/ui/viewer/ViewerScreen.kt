package app.visto.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
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
 * rest of the viewer session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    session: ViewerSession,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    mediaUrlOf: (RemoteEntry) -> String,
    cacheKeyScope: String,
    autoLoadOriginalImages: Boolean,
    onClose: () -> Unit,
) {
    if (session.items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val pagerState = rememberPagerState(initialPage = session.initialIndex) { session.items.size }
    val manualLoaded = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = session.items.getOrNull(pagerState.currentPage)
                    Text(text = current?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding: PaddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
        ) { page ->
            val item = session.items[page]
            when (item.mediaType) {
                MediaType.VIDEO -> VideoPage(item, okHttpClient, mediaUrlOf(item))
                else -> {
                    val loaded = autoLoadOriginalImages || manualLoaded[item.path] == true
                    ImagePage(
                        item = item,
                        imageLoader = imageLoader,
                        url = mediaUrlOf(item),
                        cacheKeyScope = cacheKeyScope,
                        loaded = loaded,
                        onLoadRequest = { manualLoaded[item.path] = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePage(
    item: RemoteEntry,
    imageLoader: ImageLoader,
    url: String,
    cacheKeyScope: String,
    loaded: Boolean,
    onLoadRequest: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var loadAttempt by remember(item.path) { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (loaded) Modifier.pointerInput(item.path) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        scale = newScale
                        if (newScale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                } else Modifier
            ),
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
            LoadOriginalOverlay(item, onLoadRequest)
        }
    }
}

@Composable
private fun LoadOriginalOverlay(item: RemoteEntry, onLoad: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1E)),
    ) {
        Text(
            text = OriginalImageUiText.fileDetails(item.name, item.sizeBytes),
            color = Color.White.copy(alpha = 0.56f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onLoad,
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.18f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
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
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = OriginalImageUiText.loadButtonLabel(item.sizeBytes),
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "默认不自动下载原图，点按后再加载",
                color = Color.White.copy(alpha = 0.42f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
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
