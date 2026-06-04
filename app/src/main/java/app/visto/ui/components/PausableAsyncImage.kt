package app.visto.ui.components

import android.graphics.drawable.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

/**
 * Lightweight image composable for thumbnail grids.
 *
 * Coil starts animated GIF/WebP drawables automatically when they enter the
 * composition. Visto pauses those drawables while a list/grid is scrolling so
 * animated albums keep their personality when idle without fighting scroll FPS.
 */
@Composable
fun PausableAsyncImage(
    model: Any,
    imageLoader: ImageLoader,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    playAnimations: Boolean = true,
    resumeDelayMs: Long = 0,
    loading: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = {},
) {
    val painter = rememberAsyncImagePainter(model = model, imageLoader = imageLoader)
    val state = painter.state
    val drawable = (state as? AsyncImagePainter.State.Success)?.result?.drawable
    val animatable = drawable as? Animatable

    LaunchedEffect(animatable, playAnimations, resumeDelayMs) {
        if (animatable != null) {
            if (playAnimations) {
                if (resumeDelayMs > 0) delay(resumeDelayMs)
                if (!animatable.isRunning) animatable.start()
            } else {
                animatable.stop()
            }
        }
    }
    DisposableEffect(animatable) {
        onDispose { animatable?.stop() }
    }

    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> loading()
            is AsyncImagePainter.State.Error -> error()
            else -> Unit
        }
    }
}
