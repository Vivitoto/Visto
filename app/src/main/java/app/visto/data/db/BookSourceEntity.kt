package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Saved WebDAV directory root used to scan books into the local bookshelf.
 *
 * The source row stores only directory configuration and scan metadata.
 * Imported books and reading progress continue to live in [BookProgressEntity].
 */
@Entity(
    tableName = "book_source",
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
data class BookSourceEntity(
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
    @ColumnInfo(name = "lastScannedAt")
    val lastScannedAt: Long? = null,
    @ColumnInfo(name = "lastImportedCount", defaultValue = "0")
    val lastImportedCount: Int = 0,
    @ColumnInfo(name = "lastUpdatedCount", defaultValue = "0")
    val lastUpdatedCount: Int = 0,
    @ColumnInfo(name = "lastFoldersVisited", defaultValue = "0")
    val lastFoldersVisited: Int = 0,
    @ColumnInfo(name = "lastFoldersFailed", defaultValue = "0")
    val lastFoldersFailed: Int = 0,
)
