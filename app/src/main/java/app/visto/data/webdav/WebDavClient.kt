package app.visto.data.webdav

import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Dispatcher
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

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
    private val accountId: Long,
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
        val url = buildUrl(normalized)
        val authHeader = Credentials.basic(credentials.username, credentials.password)
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE))
            .header("Authorization", authHeader)
            .header("Depth", "1")
            .header("Content-Type", "application/xml; charset=utf-8")
            .header("Accept", "application/xml, text/xml")
            .build()

        val xml = try {
            client.newCall(request).execute().use { response ->
                when (val code = response.code) {
                    in 200..299 -> response.body?.string().orEmpty()
                    401, 403 -> throw WebDavError.AuthFailed()
                    404 -> throw WebDavError.NotFound()
                    in 500..599 -> throw WebDavError.ServerError(code)
                    else -> throw WebDavError.Unexpected(code)
                }
            }
        } catch (e: SocketTimeoutException) {
            throw WebDavError.Timeout(cause = e)
        } catch (e: IOException) {
            throw WebDavError.NetworkError("Network failure during PROPFIND", e)
        }

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
     * Plain media URL for use with Coil / Media3 when authentication is
     * provided out-of-band (for example via [app.visto.data.webdav.WebDavAuthInterceptor]).
     */
    fun mediaUrl(path: String): String = buildUrl(DavPath.normalize(path)).toString()

    private fun buildUrl(absoluteVistoPath: String): HttpUrl {
        // baseUrl's path acts as the WebDAV root; absoluteVistoPath is relative to it.
        val basePath = baseUrl.encodedPath.trimEnd('/')
        val joined = if (absoluteVistoPath == "/") "$basePath/" else "$basePath$absoluteVistoPath"
        val resolved = baseUrl.newBuilder()
            .encodedPath(joined.ifEmpty { "/" })
            .build()
        return resolved
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
