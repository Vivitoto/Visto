package app.visto.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Visto media viewer.
 *
 * Images get pinch zoom and pan; videos are played with Media3/ExoPlayer.
 * The Authorization header is provided out-of-band by the shared OkHttp
 * client (used by both Coil and ExoPlayer), so the URL passed in does not
 * include credentials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    session: ViewerSession,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    onClose: () -> Unit,
) {
    if (session.items.isEmpty()) {
        onClose()
        return
    }
    val pagerState = rememberPagerState(initialPage = session.initialIndex) { session.items.size }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = session.items.getOrNull(pagerState.currentPage)
                    Text(text = current?.name ?: "")
                },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") } },
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
                MediaType.VIDEO -> VideoPage(item, mediaUrlOf(item))
                else -> ImagePage(item, imageLoader, mediaUrlOf(item))
            }
        }
    }
}

@Composable
private fun ImagePage(item: RemoteEntry, imageLoader: ImageLoader, url: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.path) {
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
            },
        contentAlignment = Alignment.Center,
    ) {
        val request = ImageRequest.Builder(LocalContext.current)
            .data(url)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            },
            error = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { Text(text = "加载失败：${item.name}") }
            },
        )
    }
}

@Composable
private fun VideoPage(item: RemoteEntry, url: String) {
    val context = LocalContext.current
    val player = remember(item.path) {
        ExoPlayer.Builder(context).build().apply {
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
