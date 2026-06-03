package app.visto.data.webdav

/**
 * Credentials for a single WebDAV account.
 *
 * The [password] field is intentionally not exposed by [toString] to avoid
 * accidental logging.
 */
data class WebDavCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
) {
    override fun toString(): String =
        "WebDavCredentials(baseUrl=<redacted>, username=<redacted>, password=<redacted>)"
}
