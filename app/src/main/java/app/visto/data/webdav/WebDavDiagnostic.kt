package app.visto.data.webdav

/** A single visible WebDAV diagnostic step. */
data class WebDavDiagnosticStep(
    val title: String,
    val status: WebDavDiagnosticStatus,
    val detail: String,
)

enum class WebDavDiagnosticStatus {
    PASS,
    FAIL,
}

/** Final diagnostic result shown in account/settings screens. */
data class WebDavDiagnosticResult(
    val ok: Boolean,
    val summary: String,
    val steps: List<WebDavDiagnosticStep>,
)
