package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent record for one WebDAV account.
 *
 * v0.1 keeps a single active account but the schema supports multiple so
 * later versions can add multi-account UI without a migration.
 *
 * Credentials are not stored here; encrypted credential storage lives behind
 * a separate abstraction so this table never contains plain passwords.
 */
@Entity(
    tableName = "dav_account",
    indices = [Index(value = ["baseUrl", "username"], unique = true)],
)
data class DavAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "displayName")
    val displayName: String,
    @ColumnInfo(name = "baseUrl")
    val baseUrl: String,
    @ColumnInfo(name = "rootPath")
    val rootPath: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "credentialRef")
    val credentialRef: String,
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = false,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
)
