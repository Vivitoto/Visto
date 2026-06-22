package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest

/** Text loaded from WebDAV and cached locally for later reader sessions. */
data class BookTextResult(
    val text: String,
    val encoding: String,
    val sizeBytes: Long,
    val cachedFile: File,
)

class BookTextLoader(
    private val webDavClient: WebDavClient,
    private val cacheDir: File,
) {
    suspend fun load(
        path: String,
        knownEtag: String? = null,
        knownSize: Long? = null,
    ): BookTextResult {
        cacheDir.mkdirs()
        val cachedFile = cacheFileFor(path)

        if (cachedFile.exists() && cachedFile.canRead() && isRemoteUnchanged(path, knownEtag, knownSize)) {
            val bytes = cachedFile.readBytes()
            val encoding = TextEncodingDetector.detect(bytes)
            return BookTextResult(
                text = decode(bytes, encoding),
                encoding = encoding,
                sizeBytes = bytes.size.toLong(),
                cachedFile = cachedFile,
            )
        }

        val bytes = webDavClient.getBytes(path)
        val encoding = TextEncodingDetector.detect(bytes)
        val text = decode(bytes, encoding)
        cachedFile.writeText(text, Charsets.UTF_8)

        return BookTextResult(
            text = text,
            encoding = encoding,
            sizeBytes = bytes.size.toLong(),
            cachedFile = cachedFile,
        )
    }

    private suspend fun isRemoteUnchanged(path: String, knownEtag: String?, knownSize: Long?): Boolean {
        if (knownEtag == null && knownSize == null) return true
        val metadata = runCatching { webDavClient.headFile(path) }.getOrNull() ?: return true
        val etagSame = knownEtag == null || normalizeEtag(metadata.etag) == normalizeEtag(knownEtag)
        val sizeSame = knownSize == null || metadata.sizeBytes == null || metadata.sizeBytes == knownSize
        return etagSame && sizeSame
    }

    private fun cacheFileFor(path: String): File = File(cacheDir, "${sanitize(path)}.txt")

    private fun sanitize(path: String): String {
        val readable = path.trim('/').replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_')
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(path.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return listOf(readable.ifEmpty { "book" }.take(48), digest).joinToString("-")
    }

    private fun decode(bytes: ByteArray, encoding: String): String {
        val charset = Charset.forName(encoding)
        val offset = when {
            encoding == "UTF-8" && bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> 3
            encoding == "UTF-16LE" && bytes.size >= 2 &&
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> 2
            encoding == "UTF-16BE" && bytes.size >= 2 &&
                bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> 2
            else -> 0
        }
        return bytes.copyOfRange(offset, bytes.size).toString(charset)
    }

    private fun normalizeEtag(value: String?): String? = value
        ?.removePrefix("W/")
        ?.trim()
        ?.trim('"')
}
