package app.visto.data.webdav

import app.visto.core.model.DavPath
import java.io.ByteArrayOutputStream
import java.net.URI

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
        val decodedPath = absoluteLike(percentDecodePath(pathOnly(rawHref)))
        val stripped = stripBasePrefix(decodedPath, basePathPrefix)
        return DavPath.normalize(stripped)
    }

    private fun absoluteLike(path: String): String =
        if (path.startsWith('/')) path else "/$path"

    private fun stripBasePrefix(decodedPath: String, basePathPrefix: String): String {
        if (basePathPrefix.isEmpty()) return decodedPath
        return when {
            decodedPath == basePathPrefix -> DavPath.ROOT
            decodedPath.startsWith("$basePathPrefix/") -> decodedPath.removePrefix(basePathPrefix)
            else -> decodedPath
        }
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
        val rawPrefix = try {
            val uri = URI(baseUrl)
            uri.rawPath.orEmpty()
        } catch (_: Exception) {
            ""
        }
        val decoded = DavPath.normalize(percentDecodePath(rawPrefix))
        return if (decoded == DavPath.ROOT) "" else decoded
    }

    /**
     * Decode percent escapes using URI path semantics.
     *
     * Do not use URLDecoder here: it implements HTML form decoding and turns
     * '+' into a space, which corrupts valid WebDAV file names like a+b.jpg.
     */
    private fun percentDecodePath(value: String): String {
        val out = StringBuilder(value.length)
        val bytes = ByteArrayOutputStream()
        var i = 0
        fun flushBytes() {
            if (bytes.size() > 0) {
                out.append(bytes.toByteArray().toString(Charsets.UTF_8))
                bytes.reset()
            }
        }
        while (i < value.length) {
            val ch = value[i]
            if (ch == '%' && i + 2 < value.length) {
                val hi = value[i + 1].digitToIntOrNull(16)
                val lo = value[i + 2].digitToIntOrNull(16)
                if (hi != null && lo != null) {
                    bytes.write((hi shl 4) + lo)
                    i += 3
                    continue
                }
            }
            flushBytes()
            out.append(ch)
            i++
        }
        flushBytes()
        return out.toString()
    }
}
