package app.visto.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeDetectorTest {

    @Test
    fun imageExtensionsAreDetectedAsImage() {
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("IMG.JPG"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("photo.jpeg"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("scan.png"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("portrait.heic"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("portrait.heif"))
    }

    @Test
    fun gifAndWebpAreAnimatedImage() {
        assertEquals(MediaType.ANIMATED_IMAGE, MediaTypeDetector.detect("party.gif"))
        assertEquals(MediaType.ANIMATED_IMAGE, MediaTypeDetector.detect("sticker.webp"))
    }

    @Test
    fun videoExtensionsAreDetectedAsVideo() {
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("clip.mp4"))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("trip.MOV"))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("vlog.webm"))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("scene.m4v"))
    }

    @Test
    fun unknownExtensionFallsBackToOther() {
        assertEquals(MediaType.OTHER, MediaTypeDetector.detect("archive.zip"))
    }

    @Test
    fun textBookExtensionsAreDetectedAsTextBook() {
        assertEquals(MediaType.TEXT_BOOK, MediaTypeDetector.detect("notes.txt"))
        assertEquals(MediaType.TEXT_BOOK, MediaTypeDetector.detect("README.md"))
    }

    @Test
    fun epubExtensionIsDetectedAsEpubBook() {
        assertEquals(MediaType.EPUB_BOOK, MediaTypeDetector.detect("novel.epub"))
    }

    @Test
    fun bookMimeTypesAreDetected() {
        assertEquals(MediaType.TEXT_BOOK, MediaTypeDetector.detect("download", mimeType = "text/plain"))
        assertEquals(MediaType.TEXT_BOOK, MediaTypeDetector.detect("download", mimeType = "text/markdown"))
        assertEquals(MediaType.EPUB_BOOK, MediaTypeDetector.detect("download", mimeType = "application/epub+zip"))
    }

    @Test
    fun missingExtensionIsUnknown() {
        assertEquals(MediaType.UNKNOWN, MediaTypeDetector.detect("README"))
        assertEquals(MediaType.UNKNOWN, MediaTypeDetector.detect(".hidden"))
        assertEquals(MediaType.UNKNOWN, MediaTypeDetector.detect("trailing."))
    }

    @Test
    fun mimeTypeBeatsExtensionWhenReliable() {
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("mystery", mimeType = "image/jpeg"))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("mystery.dat", mimeType = "video/mp4"))
        assertEquals(MediaType.ANIMATED_IMAGE, MediaTypeDetector.detect("mystery", mimeType = "image/gif"))
    }

    @Test
    fun unreliableMimeFallsBackToExtension() {
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("a.jpg", mimeType = "application/octet-stream"))
        assertEquals(MediaType.VIDEO, MediaTypeDetector.detect("a.mp4", mimeType = ""))
    }

    @Test
    fun queryAndFragmentAreStrippedBeforeExtension() {
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("photo.png?token=abc"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("photo.png#frag"))
        assertEquals(MediaType.IMAGE, MediaTypeDetector.detect("/folder/photo.JPG?x=1"))
    }
}
