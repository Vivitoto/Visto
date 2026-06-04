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

    @Test
    fun plusCharacterIsNotDecodedToSpace() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav/Photos/a+b.jpg",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/Photos/a+b.jpg", path)
    }

    @Test
    fun siblingPrefixIsNotStripped() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav2/Photos/a.jpg",
            baseUrl = "https://dav.example.com/dav",
        )
        assertEquals("/dav2/Photos/a.jpg", path)
    }

    @Test
    fun encodedHashPercentPlusUnicodeAndSpaceAreDecodedWithBasePath() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "/dav%20root/%E7%9B%B8%E5%86%8C%20A/a%2Bb%23c%25raw.jpg",
            baseUrl = "https://dav.example.com/dav%20root",
        )
        assertEquals("/相册 A/a+b#c%raw.jpg", path)
    }

    @Test
    fun absoluteUrlWithEncodedSpecialCharsIsStrippedToAccountPath() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "https://dav.example.com/dav/photos/%E7%8C%AB%20%E5%9B%BE/a%2Bb%23c%25.jpg",
            baseUrl = "https://dav.example.com/dav/photos",
        )
        assertEquals("/猫 图/a+b#c%.jpg", path)
    }

    @Test
    fun relativeHrefWithBasePrefixIsStripped() {
        val path = WebDavHrefNormalizer.toAccountPath(
            rawHref = "dav/photos/%E7%8C%AB.jpg",
            baseUrl = "https://dav.example.com/dav/photos",
        )
        assertEquals("/猫.jpg", path)
    }
}
