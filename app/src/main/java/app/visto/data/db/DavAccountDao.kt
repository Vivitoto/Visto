package app.visto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DavAccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: DavAccountEntity): Long

    @Update
    suspend fun update(account: DavAccountEntity)

    @Query("SELECT * FROM dav_account WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DavAccountEntity?

    @Query("SELECT * FROM dav_account WHERE baseUrl = :baseUrl AND username = :username LIMIT 1")
    suspend fun getByBaseUrlAndUsername(baseUrl: String, username: String): DavAccountEntity?

    @Query("SELECT * FROM dav_account WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): DavAccountEntity?

    @Query("SELECT * FROM dav_account ORDER BY updatedAt DESC")
    suspend fun getAll(): List<DavAccountEntity>

    @Query("UPDATE dav_account SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE dav_account SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markActive(id: Long, updatedAt: Long)

    @Query("DELETE FROM dav_account WHERE id = :id")
    suspend fun deleteById(id: Long)
}
