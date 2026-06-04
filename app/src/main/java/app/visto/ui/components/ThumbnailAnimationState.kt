package app.visto.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val THUMBNAIL_ANIMATION_RESUME_DELAY_MS = 220L

/**
 * Immediately pauses thumbnail animations during scroll, then resumes them
 * after a short idle delay to avoid restarting many animated WebP/GIF drawables
 * during tiny fling-state transitions.
 */
@Composable
fun rememberThumbnailAnimationsEnabled(isScrollInProgress: Boolean): Boolean {
    var enabled by remember { mutableStateOf(!isScrollInProgress) }
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            enabled = false
        } else {
            delay(THUMBNAIL_ANIMATION_RESUME_DELAY_MS)
            enabled = true
        }
    }
    return enabled
}
