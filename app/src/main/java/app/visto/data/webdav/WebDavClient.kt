package app.visto.data.webdav

import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.Dispatcher
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Read-only WebDAV client used by Visto v0.1.
 *
 * The client deliberately exposes only safe operations:
 *  - [listDirectory] performs PROPFIND with Depth: 1.
 *  - [buildMediaRequest] builds an authenticated GET request for Coil/Media3.
 *
 * It never offers DELETE/PUT/MOVE/COPY/MKCOL.
 */
class WebDavClient(
    private val credentials: WebDavCredentials,
    val accountId: Long,
    httpClient: OkHttpClient? = null,
) {

    private val client: OkHttpClient = httpClient ?: defaultClient()

    private val baseUrl: HttpUrl = credentials.baseUrl.toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Invalid WebDAV base URL")

    /**
     * PROPFIND with Depth: 1 against [path], returning the directory's child
     * entries (the self entry is filtered out).
     */
    suspend fun listDirectory(path: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        val normalized = DavPath.normalize(path)
        val url = buildUrl(normalized, collection = true)
        val authHeader = Credentials.basic(credentials.username, credentials.password)
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE))
            .header("Authorization", authHeader)
            .header("Depth", "1")
            .header("Content-Type", "application/xml; charset=utf-8")
            .header("Accept", "application/xml, text/xml")
            .build()

        val call = client.newCall(request)
        val xml = executeCancellable(call)

        val rows = WebDavMultistatusParser.parse(xml)
        WebDavListingMapper.map(
            accountId = accountId,
            baseUrl = credentials.baseUrl,
            requestedPath = normalized,
            rows = rows,
        )
    }

    /**
     * Build an authenticated GET request for the WebDAV file at [path].
     *
     * Visto passes the resulting [Request] to image and video loaders so the
     * Authorization header is applied consistently.
     */
    fun buildMediaRequest(path: String): Request {
        val url = buildUrl(DavPath.normalize(path))
        val authHeader = Credentials.basic(credentials.username, credentials.password)
        return Request.Builder()
            .url(url)
            .get()
            .header("Authorization", authHeader)
            .build()
    }

    /**
     * Download raw bytes for the WebDAV file at [path].
     */
    suspend fun getBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        val call = client.newCall(buildMediaRequest(path))
        executeCancellableBytes(call)
    }

    /**
     * Read lightweight remote metadata for cache validation.
     */
    suspend fun headFile(path: String): WebDavFileMetadata = withContext(Dispatchers.IO) {
        val mediaRequest = buildMediaRequest(path)
        val request = mediaRequest.newBuilder().head().build()
        val call = client.newCall(request)
        executeCancellableHead(call)
    }

    /**
     * Plain media URL for use with Coil / Media3 when authentication is
     * provided out-of-band (for example via [app.visto.data.webdav.WebDavAuthInterceptor]).
     */
    fun mediaUrl(path: String): String = buildUrl(DavPath.normalize(path)).toString()

    private suspend fun executeCancellable(call: Call): String =
        suspendCancellableCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { resp ->
                            if (!continuation.isActive) return
                            val code = resp.code
                            when {
                                code in 200..299 -> continuation.resume(resp.body?.string().orEmpty())
                                code == 401 || code == 403 -> continuation.resumeWithException(WebDavError.AuthFailed())
                                code == 404 -> continuation.resumeWithException(WebDavError.NotFound())
                                code in 500..599 -> continuation.resumeWithException(WebDavError.ServerError(code))
                                else -> continuation.resumeWithException(WebDavError.Unexpected(code))
                            }
                        }
                    } catch (e: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    if (e is SocketTimeoutException) {
                        continuation.resumeWithException(WebDavError.Timeout(cause = e))
                    } else {
                        continuation.resumeWithException(WebDavError.NetworkError("Network failure during PROPFIND", e))
                    }
                }
            })
            continuation.invokeOnCancellation {
                if (!call.isCanceled()) call.cancel()
            }
        }

    private suspend fun executeCancellableBytes(call: Call): ByteArray =
        suspendCancellableCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { resp ->
                            if (!continuation.isActive) return
                            val code = resp.code
                            when {
                                code in 200..299 -> continuation.resume(resp.body?.bytes() ?: ByteArray(0))
                                code == 401 || code == 403 -> continuation.resumeWithException(WebDavError.AuthFailed())
                                code == 404 -> continuation.resumeWithException(WebDavError.NotFound())
                                code in 500..599 -> continuation.resumeWithException(WebDavError.ServerError(code))
                                else -> continuation.resumeWithException(WebDavError.Unexpected(code))
                            }
                        }
                    } catch (e: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    if (e is SocketTimeoutException) {
                        continuation.resumeWithException(WebDavError.Timeout(cause = e))
                    } else {
                        continuation.resumeWithException(WebDavError.NetworkError("Network failure during GET", e))
                    }
                }
            })
            continuation.invokeOnCancellation {
                if (!call.isCanceled()) call.cancel()
            }
        }

    private suspend fun executeCancellableHead(call: Call): WebDavFileMetadata =
        suspendCancellableCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { resp ->
                            if (!continuation.isActive) return
                            val code = resp.code
                            when {
                                code in 200..299 -> continuation.resume(
                                    WebDavFileMetadata(
                                        etag = resp.header("ETag"),
                                        sizeBytes = resp.header("Content-Length")?.toLongOrNull(),
                                    ),
                                )
                                code == 401 || code == 403 -> continuation.resumeWithException(WebDavError.AuthFailed())
                                code == 404 -> continuation.resumeWithException(WebDavError.NotFound())
                                code in 500..599 -> continuation.resumeWithException(WebDavError.ServerError(code))
                                else -> continuation.resumeWithException(WebDavError.Unexpected(code))
                            }
                        }
                    } catch (e: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    if (e is SocketTimeoutException) {
                        continuation.resumeWithException(WebDavError.Timeout(cause = e))
                    } else {
                        continuation.resumeWithException(WebDavError.NetworkError("Network failure during HEAD", e))
                    }
                }
            })
            continuation.invokeOnCancellation {
                if (!call.isCanceled()) call.cancel()
            }
        }

    private fun buildUrl(absoluteVistoPath: String, collection: Boolean = false): HttpUrl {
        // baseUrl carries the WebDAV root (it may already include path segments,
        // e.g. https://nas.example.com/dav). absoluteVistoPath is decoded and
        // is appended one segment at a time so OkHttp can properly URL-encode
        // non-ASCII names (Chinese, spaces, '#', '%', etc).
        val builder = baseUrl.newBuilder()
        // Drop the implicit trailing empty segment from the base if present
        // so we don't get a double slash when we append.
        if (baseUrl.encodedPathSegments.isNotEmpty() && baseUrl.encodedPathSegments.last().isEmpty()) {
            builder.removePathSegment(baseUrl.encodedPathSegments.lastIndex)
        }
        if (absoluteVistoPath != "/") {
            absoluteVistoPath.trimStart('/').split('/').forEach { segment ->
                if (segment == "." || segment == "..") throw WebDavError.InvalidPath()
                if (segment.isNotEmpty()) builder.addPathSegment(segment)
            }
            if (collection) builder.addPathSegment("")
        } else {
            // Need the trailing slash for collection PROPFIND.
            builder.addPathSegment("")
        }
        return builder.build()
    }

    companion object {
        private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()

        private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>
<d:propfind xmlns:d="DAV:">
  <d:prop>
    <d:resourcetype/>
    <d:displayname/>
    <d:getcontentlength/>
    <d:getcontenttype/>
    <d:getetag/>
    <d:getlastmodified/>
  </d:prop>
</d:propfind>
"""

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 12
                    maxRequestsPerHost = 6
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

data class WebDavFileMetadata(
    val etag: String?,
    val sizeBytes: Long?,
)
