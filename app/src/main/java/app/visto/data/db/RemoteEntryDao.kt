package app.visto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RemoteEntryEntity>): List<Long>

    @Query("SELECT * FROM remote_entry WHERE accountId = :accountId AND parentPath = :parentPath ORDER BY sortName COLLATE NOCASE")
    suspend fun entriesForParent(accountId: Long, parentPath: String): List<RemoteEntryEntity>

    @Query("SELECT * FROM remote_entry WHERE accountId = :accountId AND parentPath IS NULL ORDER BY sortName COLLATE NOCASE")
    suspend fun rootEntries(accountId: Long): List<RemoteEntryEntity>

    @Query("SELECT * FROM remote_entry WHERE accountId = :accountId AND path = :path LIMIT 1")
    suspend fun entryByPath(accountId: Long, path: String): RemoteEntryEntity?

    @Query("DELETE FROM remote_entry WHERE accountId = :accountId AND parentPath = :parentPath")
    suspend fun deleteForParent(accountId: Long, parentPath: String)

    @Query("DELETE FROM remote_entry WHERE accountId = :accountId AND parentPath IS NULL")
    suspend fun deleteRoot(accountId: Long)

    @Query("DELETE FROM remote_entry WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)
}
