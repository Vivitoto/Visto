package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached representation of one WebDAV directory item.
 *
 * Visto never treats this table as the truth source: it is a fast view onto
 * whatever the WebDAV server returned at [lastSeenAt]. Refreshing a directory
 * replaces all rows for that `(accountId, parentPath)` pair.
 */
@Entity(
    tableName = "remote_entry",
    foreignKeys = [
        ForeignKey(
            entity = DavAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "parentPath"]),
        Index(value = ["accountId", "path"], unique = true),
    ],
)
data class RemoteEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "parentPath")
    val parentPath: String?,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "isDirectory")
    val isDirectory: Boolean,
    @ColumnInfo(name = "mediaType")
    val mediaType: String,
    @ColumnInfo(name = "mimeType")
    val mimeType: String?,
    @ColumnInfo(name = "sizeBytes")
    val sizeBytes: Long?,
    @ColumnInfo(name = "etag")
    val etag: String?,
    @ColumnInfo(name = "lastModifiedEpochMs")
    val lastModifiedEpochMs: Long?,
    @ColumnInfo(name = "lastSeenAt")
    val lastSeenAt: Long,
    @ColumnInfo(name = "sortName")
    val sortName: String,
)
