package app.visto.ui.account

import app.visto.data.webdav.WebDavError
import app.visto.ui.Strings

/**
 * Maps low-level WebDAV errors to user-facing Chinese messages for the
 * account form.
 *
 * The mapping itself stays pure so it is unit-testable; the actual strings
 * come from [Strings] so we can lift them into resources later without
 * touching the mapping logic.
 */
object AccountErrorMessages {

    fun forWebDavError(error: Throwable): String = when (error) {
        is WebDavError.AuthFailed -> Strings.ERR_AUTH
        is WebDavError.NotFound -> Strings.ERR_NOT_FOUND
        is WebDavError.ServerError -> "${Strings.ERR_SERVER}（HTTP ${error.statusCode}）"
        is WebDavError.NetworkError -> Strings.ERR_NETWORK
        is WebDavError.Timeout -> Strings.ERR_TIMEOUT
        is WebDavError.ParseError -> "${Strings.ERR_UNEXPECTED}（响应解析失败）"
        is WebDavError.Unexpected -> "${Strings.ERR_UNEXPECTED}（HTTP ${error.statusCode}）"
        else -> "${Strings.ERR_UNEXPECTED}：${error.message ?: error.javaClass.simpleName}"
    }
}
