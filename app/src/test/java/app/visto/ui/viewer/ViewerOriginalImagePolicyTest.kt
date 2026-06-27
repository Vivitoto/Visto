package app.visto.ui.viewer

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerOriginalImagePolicyTest {

    private fun item(name: String, type: MediaType) = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = false,
        mediaType = type,
    )

    @Test
    fun staticImagesCanAutoLoadAndShowOriginals() {
        val image = item("still.jpg", MediaType.IMAGE)

        assertTrue(ViewerOriginalImagePolicy.canLoadOriginal(image))
        assertTrue(ViewerOriginalImagePolicy.shouldAutoLoadOriginal(image, autoLoadOriginalImages = true))
        assertTrue(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = image,
                autoLoadOriginalImages = true,
                manuallyLoaded = false,
            ),
        )
        assertTrue(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = image,
                autoLoadOriginalImages = false,
                manuallyLoaded = true,
            ),
        )
    }

    @Test
    fun animatedImagesCanLoadOriginalsLikeStaticImages() {
        val animated = item("clip.webp", MediaType.ANIMATED_IMAGE)

        assertTrue(ViewerOriginalImagePolicy.canLoadOriginal(animated))
        assertTrue(ViewerOriginalImagePolicy.shouldAutoLoadOriginal(animated, autoLoadOriginalImages = true))
        assertFalse(ViewerOriginalImagePolicy.shouldAutoLoadOriginal(animated, autoLoadOriginalImages = false))
        assertTrue(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = animated,
                autoLoadOriginalImages = true,
                manuallyLoaded = false,
            ),
        )
        assertTrue(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = animated,
                autoLoadOriginalImages = false,
                manuallyLoaded = true,
            ),
        )
        assertFalse(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = animated,
                autoLoadOriginalImages = false,
                manuallyLoaded = false,
            ),
        )
    }

    @Test
    fun videosDoNotUseImageOriginalLoading() {
        val video = item("movie.mp4", MediaType.VIDEO)

        assertFalse(ViewerOriginalImagePolicy.canLoadOriginal(video))
        assertFalse(ViewerOriginalImagePolicy.shouldAutoLoadOriginal(video, autoLoadOriginalImages = true))
        assertFalse(
            ViewerOriginalImagePolicy.shouldShowOriginal(
                item = video,
                autoLoadOriginalImages = true,
                manuallyLoaded = true,
            ),
        )
    }
}
