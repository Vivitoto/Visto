package app.visto.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BookSourceDaoTest {

    private lateinit var db: VistoDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedAccount(name: String = "acc"): Long {
        return db.davAccountDao().insert(
            DavAccountEntity(
                displayName = name,
                baseUrl = "https://dav.example.com/dav",
                rootPath = "/",
                username = "u-$name",
                credentialRef = "ref-$name",
                createdAt = 1,
                updatedAt = 1,
            )
        )
    }

    @Test
    fun insertListFindAndCount() = runBlocking {
        val acc = seedAccount()
        val id1 = db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )
        val id2 = db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "资料", rootPath = "/Docs", createdAt = 2, updatedAt = 2)
        )

        val all = db.bookSourceDao().listForAccount(acc)

        assertEquals(listOf(id1, id2), all.map { it.id })
        assertEquals(2, db.bookSourceDao().count())
        assertEquals("小说", db.bookSourceDao().findById(id1)?.displayName)
        assertEquals(id2, db.bookSourceDao().findByAccountAndRootPath(acc, "/Docs")?.id)
    }

    @Test
    fun uniqueByAccountAndRootPath() = runBlocking {
        val acc = seedAccount()
        db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )

        var threw = false
        try {
            db.bookSourceDao().insert(
                BookSourceEntity(accountId = acc, displayName = "小说 2", rootPath = "/Books", createdAt = 2, updatedAt = 2)
            )
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            threw = true
        }

        assertTrue("expected unique constraint violation", threw)
    }

    @Test
    fun updateScanResultStoresLastSummary() = runBlocking {
        val acc = seedAccount()
        val id = db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )

        db.bookSourceDao().updateScanResult(
            id = id,
            accountId = acc,
            updatedAt = 99,
            lastScannedAt = 98,
            imported = 3,
            updated = 2,
            foldersVisited = 8,
            foldersFailed = 1,
        )

        val row = db.bookSourceDao().findById(id)
        assertNotNull(row)
        assertEquals(99L, row!!.updatedAt)
        assertEquals(98L, row.lastScannedAt)
        assertEquals(3, row.lastImportedCount)
        assertEquals(2, row.lastUpdatedCount)
        assertEquals(8, row.lastFoldersVisited)
        assertEquals(1, row.lastFoldersFailed)
    }

    @Test
    fun sourceWritesAreScopedToAccount() = runBlocking {
        val acc = seedAccount("owner")
        val otherAcc = seedAccount("other")
        val id = db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )

        db.bookSourceDao().updateScanResult(
            id = id,
            accountId = otherAcc,
            updatedAt = 99,
            lastScannedAt = 98,
            imported = 3,
            updated = 2,
            foldersVisited = 8,
            foldersFailed = 1,
        )
        db.bookSourceDao().deleteById(id, otherAcc)

        val row = db.bookSourceDao().findById(id)
        assertNotNull(row)
        assertEquals(acc, row!!.accountId)
        assertNull(row.lastScannedAt)
        assertEquals(0, row.lastImportedCount)
    }

    @Test
    fun deleteSourceDoesNotDeleteBookProgress() = runBlocking {
        val acc = seedAccount()
        val id = db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )
        db.bookProgressDao().upsert(
            BookProgressEntity(
                accountId = acc,
                path = "/Books/a.txt",
                name = "a.txt",
                sizeBytes = 1,
                etag = "e1",
                encoding = "UTF-8",
                chapterTitle = null,
                lastReadAt = 10,
                addedAt = 10,
            )
        )

        db.bookSourceDao().deleteById(id, acc)

        assertNull(db.bookSourceDao().findById(id))
        assertEquals("a.txt", db.bookProgressDao().getByPath(acc, "/Books/a.txt")?.name)
    }

    @Test
    fun cascadeOnAccountDelete() = runBlocking {
        val acc = seedAccount()
        db.bookSourceDao().insert(
            BookSourceEntity(accountId = acc, displayName = "小说", rootPath = "/Books", createdAt = 1, updatedAt = 1)
        )

        db.davAccountDao().deleteById(acc)

        assertEquals(emptyList<BookSourceEntity>(), db.bookSourceDao().listForAccount(acc))
        assertEquals(0, db.bookSourceDao().count())
    }
}
