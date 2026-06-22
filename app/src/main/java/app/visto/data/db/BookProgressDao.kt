package app.visto.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookProgressDao {

    @Upsert
    fun upsert(entity: BookProgressEntity)

    @Query("SELECT * FROM book_progress WHERE accountId = :accountId AND path = :path LIMIT 1")
    fun getByPath(accountId: Long, path: String): BookProgressEntity?

    @Query("SELECT * FROM book_progress WHERE accountId = :accountId ORDER BY lastReadAt DESC")
    fun getAllByAccount(accountId: Long): Flow<List<BookProgressEntity>>

    @Query("DELETE FROM book_progress WHERE accountId = :accountId AND path = :path")
    fun delete(accountId: Long, path: String)

    @Query("DELETE FROM book_progress WHERE accountId = :accountId")
    fun deleteByAccount(accountId: Long)
}
