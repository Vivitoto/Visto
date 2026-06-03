package app.visto.data.db

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry

/**
 * Conversion between the [RemoteEntry] domain model and its Room entity.
 *
 * Media type is stored as the enum name string so future enum additions
 * remain backward compatible.
 */
internal object RemoteEntryMappers {

    fun RemoteEntry.toEntity(lastSeenAt: Long): RemoteEntryEntity = RemoteEntryEntity(
        id = 0,
        accountId = accountId,
        parentPath = parentPath,
        path = path,
        name = name,
        isDirectory = isDirectory,
        mediaType = mediaType.name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        etag = etag,
        lastModifiedEpochMs = lastModifiedEpochMs,
        lastSeenAt = lastSeenAt,
        sortName = name.lowercase(),
    )

    fun RemoteEntryEntity.toDomain(): RemoteEntry = RemoteEntry(
        accountId = accountId,
        parentPath = parentPath,
        path = path,
        name = name,
        isDirectory = isDirectory,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.UNKNOWN),
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        etag = etag,
        lastModifiedEpochMs = lastModifiedEpochMs,
    )
}
