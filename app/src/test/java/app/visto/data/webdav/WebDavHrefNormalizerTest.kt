package app.visto.data.webdav

import org.junit.Assert.assertEquals
import org.junit.Test

class WebDavHrefNormalizerTest {

    @Test
    fun absoluteUrlIsStrippedToAccountPath() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "https://dav.example.com/dav/Photos/a.jpg",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/Photos/a.jpg", path)
    }

    @Test
    fun rootRelativeHrefIsStripped() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav/Photos/Travel/",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/Photos/Travel", path)
    }

    @Test
    fun hrefWithoutPrefixSurvivesUntouched() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/Photos/a.jpg",
            baseUrl = "https://dav.example.com",
        )
        assertEquals("/Photos/a.jpg", path)
    }

    @Test
    fun encodedSpacesAndUnicodeAreDecoded() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav/My%20Photos/%E6%97%85%E8%A1%8C/a.jpg",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/My Photos/旅行/a.jpg", path)
    }

    @Test
    fun rootHrefMapsToRoot() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav/",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/", path)
    }
}
