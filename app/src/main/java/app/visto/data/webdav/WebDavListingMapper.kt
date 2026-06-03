package app.visto.data.webdav

import app.visto.core.media.MediaType
import app.visto.core.media.MediaTypeDetector
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry

/**
 * Converts the raw [WebDavRow] entries returned by the parser into the
 * domain [RemoteEntry] objects Visto uses. Filters out the listing's own
 * "self" entry so the UI never shows a directory inside itself.
 */
object WebDavListingMapper {

    /**
     * Map [rows] returned by PROPFIND on [requestedPath] to domain entries.
     *
     * @param accountId Visto account id used to namespace cached entries.
     * @param baseUrl   WebDAV account base URL (used by href normalization).
     * @param requestedPath The Visto absolute path that was requested with PROPFIND.
     */
    fun map(
        accountId: Long,
        baseUrl: String,
        requestedPath: String,
        rows: List<WebDavRow>,
    ): List<RemoteEntry> {
        val parentPath = DavPath.normalize(requestedPath)
        val out = mutableListOf<RemoteEntry>()
        for (row in rows) {
            val path = WebDavHrefNormalizer.toAccountPath(row.rawHref, baseUrl)
            if (path == parentPath) continue // self entry
            if (!DavPath.isDirectChild(parentPath, path)) continue
            val name = row.displayName?.takeIf { it.isNotBlank() } ?: DavPath.displayName(path)
            val mediaType = if (row.isDirectory) {
                MediaType.OTHER
            } else {
                MediaTypeDetector.detect(name, row.mimeType)
            }
            out += RemoteEntry(
                accountId = accountId,
                parentPath = parentPath,
                path = path,
                name = name,
                isDirectory = row.isDirectory,
                mediaType = mediaType,
                mimeType = row.mimeType,
                sizeBytes = row.sizeBytes,
                etag = row.etag,
                lastModifiedEpochMs = row.lastModifiedEpochMs,
            )
        }
        return out
    }
}
