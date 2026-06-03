package app.visto.data.webdav

import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Tolerant parser for WebDAV multistatus responses (RFC 4918 §14.16).
 *
 * Backed by kXML2, which provides the `org.xmlpull.v1.XmlPullParser` API on
 * both Android and the JVM (so unit tests run without Robolectric).
 *
 * The parser is namespace-agnostic: WebDAV elements are matched by their
 * local name regardless of the prefix the server uses. Only propstat blocks
 * whose status line indicates a 2xx code are consumed.
 */
object WebDavMultistatusParser {

    private val HTTP_DATE_FORMATS: List<SimpleDateFormat> = listOf(
        // RFC 1123, the value WebDAV spec mandates for getlastmodified.
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        // RFC 850.
        "EEEE, dd-MMM-yy HH:mm:ss zzz",
        // ANSI C asctime().
        "EEE MMM d HH:mm:ss yyyy",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT") }
    }

    @Throws(WebDavError.ParseError::class)
    fun parse(xml: String): List<WebDavRow> {
        try {
            val parser = KXmlParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                setInput(StringReader(xml))
            }
            val rows = mutableListOf<WebDavRow>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG &&
                    parser.name.equals("response", ignoreCase = true)
                ) {
                    rows.add(readResponse(parser))
                }
                event = parser.next()
            }
            return rows.filter { it.rawHref.isNotEmpty() }
        } catch (e: XmlPullParserException) {
            throw WebDavError.ParseError("Failed to parse WebDAV multistatus XML", e)
        } catch (e: Exception) {
            throw WebDavError.ParseError("Failed to parse WebDAV multistatus XML", e)
        }
    }

    private fun readResponse(parser: XmlPullParser): WebDavRow {
        var href = ""
        var isDirectory = false
        var displayName: String? = null
        var mimeType: String? = null
        var size: Long? = null
        var etag: String? = null
        var lastModified: Long? = null

        // Currently at <response>; walk until matching END_TAG.
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("response", ignoreCase = true)) {
                break
            }
            if (event != XmlPullParser.START_TAG) continue
            when (parser.name.lowercase(Locale.ROOT)) {
                "href" -> href = readElementText(parser)
                "propstat" -> {
                    val parsed = readPropstat(parser)
                    if (parsed != null) {
                        if (parsed.isDirectory) isDirectory = true
                        if (displayName == null) displayName = parsed.displayName?.takeIf { it.isNotBlank() }
                        if (mimeType == null) mimeType = parsed.mimeType?.takeIf { it.isNotBlank() }
                        if (size == null) size = parsed.sizeBytes
                        if (etag == null) etag = parsed.etag
                        if (lastModified == null) lastModified = parsed.lastModifiedEpochMs
                    }
                }
                else -> skipElement(parser)
            }
        }

        return WebDavRow(
            rawHref = href,
            isDirectory = isDirectory,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = size,
            etag = etag,
            lastModifiedEpochMs = lastModified,
        )
    }

    private data class PropstatBlock(
        val isDirectory: Boolean,
        val displayName: String?,
        val mimeType: String?,
        val sizeBytes: Long?,
        val etag: String?,
        val lastModifiedEpochMs: Long?,
    )

    private fun readPropstat(parser: XmlPullParser): PropstatBlock? {
        var statusOk = true
        var isDirectory = false
        var displayName: String? = null
        var mimeType: String? = null
        var size: Long? = null
        var etag: String? = null
        var lastModified: Long? = null

        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("propstat", ignoreCase = true)) {
                break
            }
            if (event != XmlPullParser.START_TAG) continue
            when (parser.name.lowercase(Locale.ROOT)) {
                "status" -> {
                    val statusValue = readElementText(parser)
                    statusOk = Regex("\\b2\\d\\d\\b").containsMatchIn(statusValue)
                }
                "prop" -> readProp(parser) { name, value, collection ->
                    when (name) {
                        "displayname" -> displayName = value
                        "getcontenttype" -> mimeType = value
                        "getcontentlength" -> size = value.toLongOrNull()
                        "getetag" -> etag = value
                        "getlastmodified" -> lastModified = parseHttpDate(value)
                        "resourcetype" -> if (collection) isDirectory = true
                    }
                }
                else -> skipElement(parser)
            }
        }

        return if (statusOk) {
            PropstatBlock(isDirectory, displayName, mimeType, size, etag, lastModified)
        } else {
            null
        }
    }

    private fun readProp(
        parser: XmlPullParser,
        emit: (name: String, value: String, collection: Boolean) -> Unit,
    ) {
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("prop", ignoreCase = true)) {
                break
            }
            if (event != XmlPullParser.START_TAG) continue
            val name = parser.name.lowercase(Locale.ROOT)
            if (name == "resourcetype") {
                val collection = hasCollectionChild(parser)
                emit(name, "", collection)
            } else {
                val value = readElementText(parser)
                emit(name, value, false)
            }
        }
    }

    private fun hasCollectionChild(parser: XmlPullParser): Boolean {
        var collection = false
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals("resourcetype", ignoreCase = true)) {
                break
            }
            if (event == XmlPullParser.START_TAG) {
                if (parser.name.equals("collection", ignoreCase = true)) {
                    collection = true
                }
                skipElement(parser)
            }
        }
        return collection
    }

    private fun readElementText(parser: XmlPullParser): String {
        // Positioned at START_TAG.
        val startName = parser.name
        val sb = StringBuilder()
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals(startName, ignoreCase = true)) {
                break
            }
            when (event) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> skipElement(parser)
                XmlPullParser.END_DOCUMENT -> return sb.toString().trim()
            }
        }
        return sb.toString().trim()
    }

    private fun skipElement(parser: XmlPullParser) {
        val startName = parser.name
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name.equals(startName, ignoreCase = true)) {
                return
            }
            if (event == XmlPullParser.END_DOCUMENT) return
        }
    }

    private fun parseHttpDate(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        for (format in HTTP_DATE_FORMATS) {
            try {
                val date: Date = synchronized(format) { format.parse(text) }
                    ?: continue
                return date.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }
}
