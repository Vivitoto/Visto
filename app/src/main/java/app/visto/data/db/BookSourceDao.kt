package app.visto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookSourceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BookSourceEntity): Long

    @Query("SELECT * FROM book_source WHERE accountId = :accountId ORDER BY createdAt ASC")
    suspend fun listForAccount(accountId: Long): List<BookSourceEntity>

    @Query("SELECT COUNT(*) FROM book_source")
    suspend fun count(): Int

    @Query("SELECT * FROM book_source WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BookSourceEntity?

    @Query("SELECT * FROM book_source WHERE accountId = :accountId AND rootPath = :rootPath LIMIT 1")
    suspend fun findByAccountAndRootPath(accountId: Long, rootPath: String): BookSourceEntity?

    @Query("DELETE FROM book_source WHERE id = :id AND accountId = :accountId")
    suspend fun deleteById(id: Long, accountId: Long)

    @Query(
        """
        UPDATE book_source
        SET updatedAt = :updatedAt,
            lastScannedAt = :lastScannedAt,
            lastImportedCount = :imported,
            lastUpdatedCount = :updated,
            lastFoldersVisited = :foldersVisited,
            lastFoldersFailed = :foldersFailed
        WHERE id = :id AND accountId = :accountId
        """
    )
    suspend fun updateScanResult(
        id: Long,
        accountId: Long,
        updatedAt: Long,
        lastScannedAt: Long,
        imported: Int,
        updated: Int,
        foldersVisited: Int,
        foldersFailed: Int,
    )
}
