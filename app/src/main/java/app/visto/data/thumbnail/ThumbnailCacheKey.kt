package app.visto.data.thumbnail

import app.visto.core.model.RemoteEntry
import java.security.MessageDigest

/**
 * Builds a stable cache key for a remote media file based on its identity
 * and most recent server-reported metadata.
 *
 * Strategy:
 *  - Prefer ETag, which servers are supposed to change on content updates.
 *  - Fallback to size + last-modified.
 *  - Last resort: only the path (yields stable key but no auto-invalidation).
 *
 * The key is a hex SHA-256 over the chosen inputs so callers can safely use
 * it as a file name component or DB lookup.
 */
object ThumbnailCacheKey {

    fun forEntry(entry: RemoteEntry): String {
        val source = when {
            entry.etag != null && entry.etag.isNotBlank() ->
                "etag|${entry.accountId}|${entry.path}|${entry.etag}"
            entry.sizeBytes != null || entry.lastModifiedEpochMs != null ->
                "meta|${entry.accountId}|${entry.path}|${entry.sizeBytes ?: -1}|${entry.lastModifiedEpochMs ?: -1}"
            else -> "path|${entry.accountId}|${entry.path}"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
