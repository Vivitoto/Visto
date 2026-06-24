package app.visto.data.db

import androidx.room.Room
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
        dao = db.bookProgressDao()
    }

    @After
    fun tearDown() {
        db.close()
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

        dao.upsert(existing.copy(chapterIndex = 2, pageOffset = 7, pageStartChar = 1234, lastReadAt = 20))

        val updated = dao.getByPath(accountId, "/Books/a.txt")
        assertEquals(2, updated?.chapterIndex)
        assertEquals(7, updated?.pageOffset)
        assertEquals(1234, updated?.pageStartChar)
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
    fun updateBookMetadataPreservesReadingProgressAndSettings() = runBlocking {
        val accountId = seedAccount("metadata")
        dao.upsert(
            book(accountId, "/Books/a.txt", name = "old.txt", lastReadAt = 10).copy(
                encoding = "GBK",
                chapterIndex = 5,
                chapterTitle = "第六章",
                pageOffset = 9,
                pageStartChar = 3000,
                totalChapters = 20,
                fontSizeSp = 23,
                lineSpacing = 2.1f,
                theme = "dark",
                fontChoice = "serif",
                textColor = "ink",
                backgroundStyle = "paper",
                pageMarginTopDp = 24,
                pageMarginBottomDp = 70,
                pageMarginStartDp = 28,
                pageMarginEndDp = 32,
                addedAt = 3,
            )
        )

        dao.updateBookMetadata(
            accountId = accountId,
            path = "/Books/a.txt",
            name = "new.txt",
            sizeBytes = 4096,
            etag = "etag-new",
        )

        val updated = dao.getByPath(accountId, "/Books/a.txt")
        assertEquals("new.txt", updated?.name)
        assertEquals(4096L, updated?.sizeBytes)
        assertEquals("etag-new", updated?.etag)
        assertEquals("GBK", updated?.encoding)
        assertEquals(5, updated?.chapterIndex)
        assertEquals("第六章", updated?.chapterTitle)
        assertEquals(9, updated?.pageOffset)
        assertEquals(3000, updated?.pageStartChar)
        assertEquals(20, updated?.totalChapters)
        assertEquals(23, updated?.fontSizeSp)
        assertEquals(2.1f, updated?.lineSpacing ?: 0f, 0.0001f)
        assertEquals("dark", updated?.theme)
        assertEquals("serif", updated?.fontChoice)
        assertEquals("ink", updated?.textColor)
        assertEquals("paper", updated?.backgroundStyle)
        assertEquals(24, updated?.pageMarginTopDp)
        assertEquals(70, updated?.pageMarginBottomDp)
        assertEquals(28, updated?.pageMarginStartDp)
        assertEquals(32, updated?.pageMarginEndDp)
        assertEquals(10L, updated?.lastReadAt)
        assertEquals(3L, updated?.addedAt)
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
