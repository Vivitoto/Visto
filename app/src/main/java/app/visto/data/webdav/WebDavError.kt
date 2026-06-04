package app.visto.data.webdav

/**
 * Errors the WebDAV client can produce. v0.1 only emits these for read-only
 * operations (PROPFIND, GET, optional HEAD).
 */
sealed class WebDavError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Server returned 401 Unauthorized or 403 Forbidden. */
    class AuthFailed(message: String = "Authentication failed") : WebDavError(message)

    /** Server returned 404 Not Found. */
    class NotFound(message: String = "Path not found") : WebDavError(message)

    /** Server returned a 5xx status. */
    class ServerError(val statusCode: Int, message: String = "Server error $statusCode") : WebDavError(message)

    /** Connection or socket failure that is not a clean HTTP response. */
    class NetworkError(message: String, cause: Throwable? = null) : WebDavError(message, cause)

    /** Request or read timed out. */
    class Timeout(message: String = "Request timed out", cause: Throwable? = null) : WebDavError(message, cause)

    /** Response body could not be parsed into a WebDAV multistatus document. */
    class ParseError(message: String, cause: Throwable? = null) : WebDavError(message, cause)

    /** User/model path contains dot segments that could escape the WebDAV base. */
    class InvalidPath(message: String = "Invalid WebDAV path") : WebDavError(message)

    /** Any other non-success HTTP status. */
    class Unexpected(val statusCode: Int, message: String = "Unexpected HTTP $statusCode") : WebDavError(message)
}
