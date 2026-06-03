package app.visto.data.webdav

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches Basic Authorization to outbound requests
 * whose URL host+path prefix matches a registered WebDAV account.
 *
 * The interceptor is intentionally a no-op for unrelated requests so it can
 * sit on a shared OkHttpClient (used by both [WebDavClient] and Coil).
 */
class WebDavAuthInterceptor(
    @Volatile private var accountBaseUrl: String? = null,
    @Volatile private var username: String? = null,
    @Volatile private var password: String? = null,
) : Interceptor {

    @Synchronized
    fun setAccount(baseUrl: String?, username: String?, password: String?) {
        this.accountBaseUrl = baseUrl
        this.username = username
        this.password = password
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (req.header("Authorization") != null) {
            return chain.proceed(req)
        }
        val baseUrl = accountBaseUrl ?: return chain.proceed(req)
        val user = username ?: return chain.proceed(req)
        val pass = password ?: return chain.proceed(req)

        val base = baseUrl.toHttpUrlOrNull() ?: return chain.proceed(req)
        val target = req.url
        if (target.host != base.host) return chain.proceed(req)
        val basePathPrefix = base.encodedPath.trimEnd('/')
        if (basePathPrefix.isNotEmpty() && !target.encodedPath.startsWith(basePathPrefix)) {
            return chain.proceed(req)
        }
        val authed = req.newBuilder()
            .header("Authorization", Credentials.basic(user, pass))
            .build()
        return chain.proceed(authed)
    }
}
