package app.visto.data.webdav

import app.visto.core.model.DavPath
import java.net.URI
import java.net.URLDecoder

/**
 * Normalizes WebDAV `href` values into absolute Visto paths.
 *
 * WebDAV servers return href in several styles:
 *  - Absolute URL: `https://dav.example.com/Photos/a.jpg`.
 *  - Root-relative: `/dav/Photos/a.jpg`.
 *  - Already-stripped: `/Photos/a.jpg`.
 *
 * Visto stores paths relative to the WebDAV account base URL's path component.
 */
object WebDavHrefNormalizer {

    /**
     * Convert [rawHref] into an absolute Visto path (always starts with '/').
     *
     * [baseUrl] is the WebDAV account base URL, used to determine the path
     * prefix that should be stripped (so that account base `/dav` becomes the
     * Visto root `/`).
     */
    fun toAccountPath(rawHref: String, baseUrl: String): String {
        val basePathPrefix = pathPrefixOf(baseUrl)
        val rawPath = pathOnly(rawHref)
        val decoded = safeUrlDecode(rawPath)
        val stripped = if (basePathPrefix.isNotEmpty() && decoded.startsWith(basePathPrefix)) {
            decoded.removePrefix(basePathPrefix)
        } else {
            decoded
        }
        return DavPath.normalize(stripped)
    }

    private fun pathOnly(rawHref: String): String {
        return try {
            val uri = URI(rawHref)
            uri.rawPath ?: rawHref
        } catch (_: Exception) {
            // Some servers return invalid URIs; treat as-is.
            rawHref
        }
    }

    private fun pathPrefixOf(baseUrl: String): String {
        val prefix = try {
            val uri = URI(baseUrl)
            uri.rawPath.orEmpty()
        } catch (_: Exception) {
            ""
        }
        return prefix.trimEnd('/')
    }

    private fun safeUrlDecode(value: String): String = try {
        URLDecoder.decode(value, Charsets.UTF_8)
    } catch (_: Exception) {
        value
    }
}
