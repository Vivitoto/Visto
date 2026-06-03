package app.visto.data.db

import androidx.room.withTransaction
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import app.visto.data.db.RemoteEntryMappers.toDomain
import app.visto.data.db.RemoteEntryMappers.toEntity

/**
 * Repository that exposes [RemoteEntry] caching as a coherent unit.
 *
 * The replace flow runs in a Room transaction so a refresh either fully
 * applies (deleting stale entries, inserting new ones) or has no effect.
 */
class RemoteEntryRepository(
    private val database: VistoDatabase,
    private val dao: RemoteEntryDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun entriesForParent(accountId: Long, parentPath: String?): List<RemoteEntry> {
        val rows = if (parentPath == null) {
            dao.rootEntries(accountId)
        } else {
            dao.entriesForParent(accountId, DavPath.normalize(parentPath))
        }
        return rows.map { it.toDomain() }
    }

    suspend fun entryByPath(accountId: Long, path: String): RemoteEntry? =
        dao.entryByPath(accountId, DavPath.normalize(path))?.toDomain()

    /**
     * Replace all cached entries directly under [parentPath] with [entries].
     */
    suspend fun replaceDirectoryListing(
        accountId: Long,
        parentPath: String?,
        entries: List<RemoteEntry>,
    ) {
        val now = clock()
        val normalizedParent = parentPath?.let { DavPath.normalize(it) }
        database.withTransaction {
            if (normalizedParent == null) {
                dao.deleteRoot(accountId)
            } else {
                dao.deleteForParent(accountId, normalizedParent)
            }
            if (entries.isNotEmpty()) {
                dao.insertAll(entries.map { it.toEntity(lastSeenAt = now) })
            }
        }
    }
}
