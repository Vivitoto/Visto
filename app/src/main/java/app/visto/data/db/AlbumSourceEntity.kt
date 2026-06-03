package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Album source: a saved WebDAV path that Visto presents as one album on the
 * home screen.
 *
 * Opening an album triggers a recursive PROPFIND walk starting at [rootPath];
 * the resulting media are grouped by their original subfolder for display.
 */
@Entity(
    tableName = "album_source",
    foreignKeys = [
        ForeignKey(
            entity = DavAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "rootPath"], unique = true),
    ],
)
data class AlbumSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "displayName")
    val displayName: String,
    @ColumnInfo(name = "rootPath")
    val rootPath: String,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
)
