package app.visto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AlbumSourceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AlbumSourceEntity): Long

    @Query("SELECT * FROM album_source WHERE accountId = :accountId ORDER BY createdAt ASC")
    suspend fun listForAccount(accountId: Long): List<AlbumSourceEntity>

    @Query("SELECT COUNT(*) FROM album_source")
    suspend fun count(): Int

    @Query("SELECT * FROM album_source WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AlbumSourceEntity?

    @Query("DELETE FROM album_source WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE album_source SET displayName = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameById(id: Long, name: String, updatedAt: Long)
}
