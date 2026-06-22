package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Properties

/** Decoded text and local cache information for a downloaded book file. */
data class BookTextResult(
    val text: String,
    val encoding: String,
    val sizeBytes: Long,
    val etag: String?,
    val cachedFile: File,
)

/** Downloads WebDAV text books, detects their encoding, and stores UTF-8 decoded cache files. */
object BookTextLoader {
    private const val CACHE_SUBDIR = "books"
    private const val META_SUFFIX = ".meta"
    private val httpClient = OkHttpClient()

    suspend fun load(
        webDavClient: WebDavClient,
        path: String,
        cacheDir: File,
        expectedEtag: String? = null,
    ): BookTextResult = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(cacheDir, webDavClient.accountId, path)
        val metaFile = metaFile(cacheFile)
        val metadata = readMetadata(metaFile)

        if (cacheFile.exists() && expectedEtag != null && metadata.etag == expectedEtag) {
            val text = cacheFile.readText(Charsets.UTF_8)
            return@withContext BookTextResult(
                text = text,
                encoding = metadata.encoding ?: "UTF-8",
                sizeBytes = metadata.sizeBytes ?: cacheFile.length(),
                etag = metadata.etag,
                cachedFile = cacheFile,
            )
        }

        cacheFile.parentFile?.mkdirs()
        val request = webDavClient.buildMediaRequest(path)
        val response = httpClient.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("GET $path failed with HTTP ${resp.code}")
            }
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            val encoding = TextEncodingDetector.detect(bytes)
            val text = String(bytes, Charset.forName(encoding)).removePrefix("\uFEFF")
            cacheFile.writeText(text, Charsets.UTF_8)
            val etag = resp.header("ETag")
            writeMetadata(
                metaFile = metaFile,
                etag = etag,
                encoding = encoding,
                sizeBytes = bytes.size.toLong(),
            )
            BookTextResult(
                text = text,
                encoding = encoding,
                sizeBytes = bytes.size.toLong(),
                etag = etag,
                cachedFile = cacheFile,
            )
        }
    }

    private fun cacheFile(cacheDir: File, accountId: Long, path: String): File =
        File(File(File(cacheDir, CACHE_SUBDIR), accountId.toString()), "${sha256(path)}.txt")

    private fun metaFile(cacheFile: File): File = File(cacheFile.parentFile, "${cacheFile.name}$META_SUFFIX")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun readMetadata(file: File): CacheMetadata {
        if (!file.exists()) return CacheMetadata()
        val props = Properties()
        runCatching { file.inputStream().use(props::load) }
        return CacheMetadata(
            etag = props.getProperty("etag"),
            encoding = props.getProperty("encoding"),
            sizeBytes = props.getProperty("sizeBytes")?.toLongOrNull(),
        )
    }

    private fun writeMetadata(
        metaFile: File,
        etag: String?,
        encoding: String,
        sizeBytes: Long,
    ) {
        metaFile.parentFile?.mkdirs()
        val props = Properties().apply {
            etag?.let { setProperty("etag", it) }
            setProperty("encoding", encoding)
            setProperty("sizeBytes", sizeBytes.toString())
        }
        metaFile.outputStream().use { props.store(it, null) }
    }

    private data class CacheMetadata(
        val etag: String? = null,
        val encoding: String? = null,
        val sizeBytes: Long? = null,
    )
}
