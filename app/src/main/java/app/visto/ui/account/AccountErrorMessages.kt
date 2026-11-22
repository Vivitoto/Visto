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

    fun forWebDavError(error: Throwable, isOnline: Boolean = true): String = when (error) {
        is WebDavError.AuthFailed -> Strings.ERR_AUTH
        is WebDavError.NotFound -> Strings.ERR_NOT_FOUND
        is WebDavError.ServerError -> "${Strings.ERR_SERVER}（HTTP ${error.statusCode}）"
        is WebDavError.NetworkError -> if (isOnline) Strings.ERR_NETWORK else Strings.ERR_OFFLINE
        is WebDavError.Timeout -> if (isOnline) Strings.ERR_TIMEOUT else Strings.ERR_OFFLINE
        is WebDavError.ParseError -> "${Strings.ERR_UNEXPECTED}（响应解析失败）"
        is WebDavError.InvalidPath -> Strings.ERR_INVALID_PATH
        is WebDavError.Unexpected -> "${Strings.ERR_UNEXPECTED}（HTTP ${error.statusCode}）"
        else -> "${Strings.ERR_UNEXPECTED}：${error.message ?: error.javaClass.simpleName}"
    }
}
