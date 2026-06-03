package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One generated thumbnail entry. Visto tracks thumbnails in the database so
 * cache cleanup can apply an LRU policy across many remote files.
 *
 * Thumbnails are local-only and never uploaded to WebDAV.
 */
@Entity(
    tableName = "thumbnail_cache",
    foreignKeys = [
        ForeignKey(
            entity = RemoteEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["remoteEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["remoteEntryId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["lastAccessedAt"]),
    ],
)
data class ThumbnailCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "remoteEntryId")
    val remoteEntryId: Long,
    @ColumnInfo(name = "cacheKey")
    val cacheKey: String,
    @ColumnInfo(name = "thumbPath")
    val thumbPath: String?,
    @ColumnInfo(name = "width")
    val width: Int?,
    @ColumnInfo(name = "height")
    val height: Int?,
    @ColumnInfo(name = "sourceEtag")
    val sourceEtag: String?,
    @ColumnInfo(name = "sourceSizeBytes")
    val sourceSizeBytes: Long?,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
    @ColumnInfo(name = "lastAccessedAt")
    val lastAccessedAt: Long,
    @ColumnInfo(name = "bytesOnDisk")
    val bytesOnDisk: Long,
    @ColumnInfo(name = "status")
    val status: String,
)
