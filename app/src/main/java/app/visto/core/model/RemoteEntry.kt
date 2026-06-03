package app.visto.core.model

import app.visto.core.media.MediaType

/**
 * One item discovered on a WebDAV server, normalized for Visto.
 *
 * Path conventions:
 *  - [path] is absolute and starts with '/'.
 *  - For directories, [path] never has a trailing slash except for the root '/'.
 *  - [parentPath] is the absolute path of the containing directory, or null for the root.
 *
 * v0.1 is read-only; no mutation fields live on this model.
 */
data class RemoteEntry(
    val accountId: Long,
    val parentPath: String?,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val mediaType: MediaType,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val etag: String? = null,
    val lastModifiedEpochMs: Long? = null,
) {
    init {
        require(accountId >= 0) { "accountId must be non-negative" }
        require(path.startsWith("/")) { "path must be absolute: $path" }
        require(name.isNotEmpty()) { "name must not be empty" }
        if (isDirectory) {
            require(mediaType == MediaType.OTHER || mediaType == MediaType.UNKNOWN) {
                "directory entry must not have a media type: $mediaType"
            }
        }
        if (parentPath != null) {
            require(parentPath.startsWith("/")) { "parentPath must be absolute: $parentPath" }
        }
    }
}
