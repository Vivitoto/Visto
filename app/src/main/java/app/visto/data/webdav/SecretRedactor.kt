package app.visto.data.webdav

/**
 * Strip secrets from URLs and headers before logging or surfacing in errors.
 *
 * v0.1 does not enable verbose network logging; this helper is intended for
 * use by error messages and future log adapters so credentials never leak.
 */
object SecretRedactor {

    private val urlCredentials = Regex("([a-z][a-z0-9+.-]*://)([^:@/]+:[^@/]+)@", RegexOption.IGNORE_CASE)
    private val tokenQuery = Regex("([?&](?:token|access_token|auth|signature)=)[^&#]+", RegexOption.IGNORE_CASE)
    private val basicAuthHeader = Regex("(?i)(Authorization:\\s*Basic\\s+)[A-Za-z0-9+/=]+")
    private val bearerAuthHeader = Regex("(?i)(Authorization:\\s*Bearer\\s+)[A-Za-z0-9._\\-]+")

    fun redactUrl(value: String): String {
        return value
            .replace(urlCredentials) { match ->
                "${match.groupValues[1]}<redacted>@"
            }
            .replace(tokenQuery) { match ->
                "${match.groupValues[1]}<redacted>"
            }
    }

    fun redactLogLine(value: String): String {
        return value
            .let(::redactUrl)
            .replace(basicAuthHeader) { match -> "${match.groupValues[1]}<redacted>" }
            .replace(bearerAuthHeader) { match -> "${match.groupValues[1]}<redacted>" }
    }
}
