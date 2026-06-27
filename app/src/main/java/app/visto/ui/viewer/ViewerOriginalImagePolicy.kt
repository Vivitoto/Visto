package app.visto.ui.viewer

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry

internal object ViewerOriginalImagePolicy {
    fun canLoadOriginal(item: RemoteEntry): Boolean =
        item.mediaType == MediaType.IMAGE || item.mediaType == MediaType.ANIMATED_IMAGE

    fun shouldAutoLoadOriginal(
        item: RemoteEntry,
        autoLoadOriginalImages: Boolean,
    ): Boolean = autoLoadOriginalImages && canLoadOriginal(item)

    fun shouldShowOriginal(
        item: RemoteEntry,
        autoLoadOriginalImages: Boolean,
        manuallyLoaded: Boolean,
    ): Boolean = canLoadOriginal(item) && (autoLoadOriginalImages || manuallyLoaded)
}
