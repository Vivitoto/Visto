package app.visto.ui.account

import app.visto.data.webdav.WebDavError

/**
 * Maps low-level WebDAV errors to user-facing messages for the account form.
 *
 * Strings are kept plain (no resource references) so this is unit-testable.
 * Actual UI strings can be swapped to string resources later without touching
 * the mapping logic.
 */
object AccountErrorMessages {

    fun forWebDavError(error: Throwable): String = when (error) {
        is WebDavError.AuthFailed -> "Authentication failed. Check the username and password."
        is WebDavError.NotFound -> "The root path was not found on the server."
        is WebDavError.ServerError -> "The WebDAV server returned an error (${error.statusCode})."
        is WebDavError.NetworkError -> "Cannot reach the WebDAV server."
        is WebDavError.Timeout -> "The WebDAV server did not respond in time."
        is WebDavError.ParseError -> "The WebDAV response could not be parsed."
        is WebDavError.Unexpected -> "Unexpected response from the server (HTTP ${error.statusCode})."
        else -> "Connection failed: ${error.message ?: error.javaClass.simpleName}"
    }
}
