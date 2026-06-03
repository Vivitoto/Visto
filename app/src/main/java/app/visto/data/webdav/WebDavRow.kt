package app.visto.data.webdav

/**
 * One row parsed from a WebDAV multistatus response, before mapping to a
 * domain [app.visto.core.model.RemoteEntry]. The parser stays small and
 * format-tolerant; classification happens in [WebDavListingMapper].
 */
data class WebDavRow(
    /** Raw href as returned by the server (may be absolute URL or root-relative). */
    val rawHref: String,
    val isDirectory: Boolean,
    val displayName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val etag: String? = null,
    val lastModifiedEpochMs: Long? = null,
)
