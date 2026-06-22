package app.visto.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BookProgressDaoTest {

    private lateinit var db: VistoDatabase
    private lateinit var dao: BookProgressDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = createBookProgressDao(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createBookProgressDao(database: RoomDatabase): BookProgressDao {
        val daoClass = Class.forName("app.visto.data.db.BookProgressDao_Impl")
        val constructor = daoClass.getDeclaredConstructor(RoomDatabase::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(database) as BookProgressDao
    }

    private suspend fun seedAccount(name: String): Long {
        return db.davAccountDao().insert(
            DavAccountEntity(
                displayName = name,
                baseUrl = "https://dav.example.com/$name",
                rootPath = "/",
                username = name,
                credentialRef = "ref-$name",
                createdAt = 1,
                updatedAt = 1,
            )
        )
    }

    private fun book(
        accountId: Long,
        path: String,
        name: String = path.substringAfterLast('/'),
        lastReadAt: Long = 100,
    ): BookProgressEntity {
        return BookProgressEntity(
            accountId = accountId,
            path = path,
            name = name,
            sizeBytes = 2048,
            etag = "etag-$name",
            encoding = "UTF-8",
            chapterTitle = "第一章",
            totalChapters = 3,
            lastReadAt = lastReadAt,
            addedAt = 1,
        )
    }

    @Test
    fun upsertInsertsAndGetByPathReturnsProgress() = runBlocking {
        val accountId = seedAccount("insert")

        dao.upsert(book(accountId, "/Books/a.txt", name = "a.txt"))

        val saved = dao.getByPath(accountId, "/Books/a.txt")
        assertEquals("a.txt", saved?.name)
        assertEquals("UTF-8", saved?.encoding)
        assertEquals(0, saved?.chapterIndex)
    }

    @Test
    fun upsertUpdatesExistingProgressByPrimaryKey() = runBlocking {
        val accountId = seedAccount("update")
        dao.upsert(book(accountId, "/Books/a.txt", lastReadAt = 10))
        val existing = dao.getByPath(accountId, "/Books/a.txt")!!

        dao.upsert(existing.copy(chapterIndex = 2, pageOffset = 7, lastReadAt = 20))

        val updated = dao.getByPath(accountId, "/Books/a.txt")
        assertEquals(2, updated?.chapterIndex)
        assertEquals(7, updated?.pageOffset)
        assertEquals(20L, updated?.lastReadAt)
    }

    @Test
    fun getAllByAccountOrdersByLastReadAtDescending() = runBlocking {
        val accountId = seedAccount("ordered")
        dao.upsert(book(accountId, "/Books/old.txt", name = "old", lastReadAt = 10))
        dao.upsert(book(accountId, "/Books/new.txt", name = "new", lastReadAt = 30))
        dao.upsert(book(accountId, "/Books/mid.txt", name = "mid", lastReadAt = 20))

        val all = dao.getAllByAccount(accountId).first()

        assertEquals(listOf("new", "mid", "old"), all.map { it.name })
    }

    @Test
    fun deleteRemovesOnePathForAccount() = runBlocking {
        val accountId = seedAccount("delete")
        dao.upsert(book(accountId, "/Books/a.txt"))
        dao.upsert(book(accountId, "/Books/b.txt"))

        dao.delete(accountId, "/Books/a.txt")

        assertNull(dao.getByPath(accountId, "/Books/a.txt"))
        assertEquals("b.txt", dao.getByPath(accountId, "/Books/b.txt")?.name)
    }

    @Test
    fun deleteByAccountRemovesOnlyThatAccountsRows() = runBlocking {
        val accountA = seedAccount("accountA")
        val accountB = seedAccount("accountB")
        dao.upsert(book(accountA, "/Books/a.txt"))
        dao.upsert(book(accountB, "/Books/b.txt"))

        dao.deleteByAccount(accountA)

        assertEquals(emptyList<BookProgressEntity>(), dao.getAllByAccount(accountA).first())
        assertEquals(listOf("b.txt"), dao.getAllByAccount(accountB).first().map { it.name })
    }
}
