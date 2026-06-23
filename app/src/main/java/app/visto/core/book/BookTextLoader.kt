package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Properties
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    private const val NORMALIZER_VERSION = "2"
    private val httpClient = OkHttpClient()

    suspend fun load(
        webDavClient: WebDavClient,
        path: String,
        cacheDir: File,
        expectedEtag: String? = null,
    ): BookTextResult = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(cacheDir, webDavClient.accountId, path)
        val metaFile = metaFile(cacheFile)

        readCachedResult(
            cacheFile = cacheFile,
            metaFile = metaFile,
            expectedEtag = expectedEtag,
            allowStale = false,
            allowLegacyCache = false,
        )?.let { return@withContext it }

        val downloaded = try {
            download(webDavClient.buildMediaRequest(path), path)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            readCachedResult(
                cacheFile = cacheFile,
                metaFile = metaFile,
                expectedEtag = expectedEtag,
                allowStale = true,
                allowLegacyCache = true,
            )?.let { return@withContext it }
            throw e
        }

        val encoding = TextEncodingDetector.detect(downloaded.bytes)
        val decodedText = String(downloaded.bytes, Charset.forName(encoding)).removePrefix("\uFEFF")
        val text = ReaderTextNormalizer.normalize(decodedText)
        writeTextAtomically(cacheFile, text)
        writeMetadata(
            metaFile = metaFile,
            etag = downloaded.etag,
            encoding = encoding,
            sizeBytes = downloaded.bytes.size.toLong(),
            normalizerVersion = NORMALIZER_VERSION,
        )
        BookTextResult(
            text = text,
            encoding = encoding,
            sizeBytes = downloaded.bytes.size.toLong(),
            etag = downloaded.etag,
            cachedFile = cacheFile,
        )
    }

    private fun readCachedResult(
        cacheFile: File,
        metaFile: File,
        expectedEtag: String?,
        allowStale: Boolean,
        allowLegacyCache: Boolean,
    ): BookTextResult? {
        if (!cacheFile.isFile) return null
        val metadata = readMetadata(metaFile)
        val needsNormalization = metadata.normalizerVersion != NORMALIZER_VERSION
        if (needsNormalization && !allowLegacyCache) return null
        val etagCompatible = expectedEtag == null || metadata.etag == null || metadata.etag == expectedEtag
        if (!etagCompatible && !allowStale) return null

        return runCatching {
            val cachedText = cacheFile.readText(Charsets.UTF_8)
            val text = if (needsNormalization) {
                ReaderTextNormalizer.normalize(cachedText).also { normalized ->
                    if (normalized != cachedText) {
                        writeTextAtomically(cacheFile, normalized)
                    }
                }
            } else {
                cachedText
            }
            val etag = metadata.etag ?: expectedEtag
            val encoding = metadata.encoding ?: "UTF-8"
            val sizeBytes = metadata.sizeBytes ?: cacheFile.length()
            if (
                needsNormalization ||
                metadata.etag != etag ||
                metadata.encoding == null ||
                metadata.sizeBytes == null
            ) {
                writeMetadata(
                    metaFile = metaFile,
                    etag = etag,
                    encoding = encoding,
                    sizeBytes = sizeBytes,
                    normalizerVersion = NORMALIZER_VERSION,
                )
            }
            BookTextResult(
                text = text,
                encoding = encoding,
                sizeBytes = sizeBytes,
                etag = etag,
                cachedFile = cacheFile,
            )
        }.getOrNull()
    }

    private suspend fun download(request: Request, path: String): DownloadedBook =
        suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { resp ->
                            if (!continuation.isActive) return
                            if (!resp.isSuccessful) {
                                continuation.resumeWithException(IOException("GET $path failed with HTTP ${resp.code}"))
                                return
                            }
                            val bytes = resp.body?.bytes() ?: ByteArray(0)
                            if (!continuation.isActive) return
                            continuation.resume(
                                DownloadedBook(
                                    bytes = bytes,
                                    etag = resp.header("ETag"),
                                )
                            )
                        }
                    } catch (e: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            })
            continuation.invokeOnCancellation {
                if (!call.isCanceled()) call.cancel()
            }
        }

    private fun writeTextAtomically(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
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
            normalizerVersion = props.getProperty("normalizerVersion"),
        )
    }

    private fun writeMetadata(
        metaFile: File,
        etag: String?,
        encoding: String,
        sizeBytes: Long,
        normalizerVersion: String,
    ) {
        metaFile.parentFile?.mkdirs()
        val props = Properties().apply {
            etag?.let { setProperty("etag", it) }
            setProperty("encoding", encoding)
            setProperty("sizeBytes", sizeBytes.toString())
            setProperty("normalizerVersion", normalizerVersion)
        }
        metaFile.outputStream().use { props.store(it, null) }
    }

    private data class CacheMetadata(
        val etag: String? = null,
        val encoding: String? = null,
        val sizeBytes: Long? = null,
        val normalizerVersion: String? = null,
    )

    private data class DownloadedBook(
        val bytes: ByteArray,
        val etag: String?,
    )
}
