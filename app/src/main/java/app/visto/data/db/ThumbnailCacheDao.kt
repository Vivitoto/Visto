package app.visto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ThumbnailCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ThumbnailCacheEntity): Long

    @Query("SELECT * FROM thumbnail_cache WHERE remoteEntryId = :remoteEntryId LIMIT 1")
    suspend fun byRemoteEntry(remoteEntryId: Long): ThumbnailCacheEntity?

    @Query("UPDATE thumbnail_cache SET lastAccessedAt = :accessedAt WHERE remoteEntryId = :remoteEntryId")
    suspend fun touch(remoteEntryId: Long, accessedAt: Long)

    @Query("SELECT * FROM thumbnail_cache WHERE status = 'ready' ORDER BY lastAccessedAt ASC")
    suspend fun readyByOldestAccess(): List<ThumbnailCacheEntity>

    @Query("SELECT COALESCE(SUM(bytesOnDisk), 0) FROM thumbnail_cache WHERE status = 'ready'")
    suspend fun totalBytesOnDisk(): Long

    @Query("DELETE FROM thumbnail_cache WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM thumbnail_cache")
    suspend fun deleteAll()
}
