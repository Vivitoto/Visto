package app.visto.data.webdav

import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * Tolerant parser for WebDAV multistatus responses (RFC 4918 §14.16).
 *
 * Uses the JVM's built-in StAX (`javax.xml.stream`) so it runs in plain JVM
 * unit tests without Robolectric or Android stubs. The parser ignores XML
 * namespaces and matches WebDAV elements by their local name only.
 *
 * Only propstat blocks whose status line indicates a 2xx code are consumed.
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

    private val xmlInputFactory: XMLInputFactory by lazy {
        XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        }
    }

    @Throws(WebDavError.ParseError::class)
    fun parse(xml: String): List<WebDavRow> {
        val reader = try {
            xmlInputFactory.createXMLStreamReader(StringReader(xml))
        } catch (e: Exception) {
            throw WebDavError.ParseError("Failed to start WebDAV XML parser", e)
        }
        try {
            val rows = mutableListOf<WebDavRow>()
            while (reader.hasNext()) {
                val event = reader.next()
                if (event == XMLStreamConstants.START_ELEMENT &&
                    reader.localName.equals("response", ignoreCase = true)
                ) {
                    rows.add(readResponse(reader))
                }
            }
            return rows.filter { it.rawHref.isNotEmpty() }
        } catch (e: Exception) {
            throw WebDavError.ParseError("Failed to parse WebDAV multistatus XML", e)
        } finally {
            try { reader.close() } catch (_: Exception) {}
        }
    }

    private fun readResponse(reader: XMLStreamReader): WebDavRow {
        var href = ""
        var isDirectory = false
        var displayName: String? = null
        var mimeType: String? = null
        var size: Long? = null
        var etag: String? = null
        var lastModified: Long? = null

        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals("response", ignoreCase = true)
            ) {
                break
            }
            if (event != XMLStreamConstants.START_ELEMENT) continue
            when (reader.localName.lowercase(Locale.ROOT)) {
                "href" -> href = readElementText(reader)
                "propstat" -> {
                    val parsed = readPropstat(reader)
                    if (parsed != null) {
                        if (parsed.isDirectory) isDirectory = true
                        if (displayName == null) displayName = parsed.displayName?.takeIf { it.isNotBlank() }
                        if (mimeType == null) mimeType = parsed.mimeType?.takeIf { it.isNotBlank() }
                        if (size == null) size = parsed.sizeBytes
                        if (etag == null) etag = parsed.etag
                        if (lastModified == null) lastModified = parsed.lastModifiedEpochMs
                    }
                }
                else -> skipElement(reader)
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

    private fun readPropstat(reader: XMLStreamReader): PropstatBlock? {
        var statusOk = true
        var isDirectory = false
        var displayName: String? = null
        var mimeType: String? = null
        var size: Long? = null
        var etag: String? = null
        var lastModified: Long? = null

        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals("propstat", ignoreCase = true)
            ) {
                break
            }
            if (event != XMLStreamConstants.START_ELEMENT) continue
            when (reader.localName.lowercase(Locale.ROOT)) {
                "status" -> {
                    val statusValue = readElementText(reader)
                    statusOk = Regex("\\b2\\d\\d\\b").containsMatchIn(statusValue)
                }
                "prop" -> readProp(reader) { name, value, collection ->
                    when (name) {
                        "displayname" -> displayName = value
                        "getcontenttype" -> mimeType = value
                        "getcontentlength" -> size = value.toLongOrNull()
                        "getetag" -> etag = value
                        "getlastmodified" -> lastModified = parseHttpDate(value)
                        "resourcetype" -> if (collection) isDirectory = true
                    }
                }
                else -> skipElement(reader)
            }
        }

        return if (statusOk) {
            PropstatBlock(isDirectory, displayName, mimeType, size, etag, lastModified)
        } else {
            null
        }
    }

    private fun readProp(
        reader: XMLStreamReader,
        emit: (name: String, value: String, collection: Boolean) -> Unit,
    ) {
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals("prop", ignoreCase = true)
            ) {
                break
            }
            if (event != XMLStreamConstants.START_ELEMENT) continue
            val name = reader.localName.lowercase(Locale.ROOT)
            if (name == "resourcetype") {
                val collection = hasCollectionChild(reader)
                emit(name, "", collection)
            } else {
                val value = readElementText(reader)
                emit(name, value, false)
            }
        }
    }

    private fun hasCollectionChild(reader: XMLStreamReader): Boolean {
        var collection = false
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals("resourcetype", ignoreCase = true)
            ) {
                break
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.localName.equals("collection", ignoreCase = true)) {
                    collection = true
                }
                skipElement(reader)
            }
        }
        return collection
    }

    private fun readElementText(reader: XMLStreamReader): String {
        val startName = reader.localName
        val sb = StringBuilder()
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals(startName, ignoreCase = true)
            ) {
                break
            }
            when (event) {
                XMLStreamConstants.CHARACTERS,
                XMLStreamConstants.CDATA,
                XMLStreamConstants.SPACE -> sb.append(reader.text)
                XMLStreamConstants.START_ELEMENT -> skipElement(reader)
            }
        }
        return sb.toString().trim()
    }

    private fun skipElement(reader: XMLStreamReader) {
        val startName = reader.localName
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == XMLStreamConstants.END_ELEMENT &&
                reader.localName.equals(startName, ignoreCase = true)
            ) {
                return
            }
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
