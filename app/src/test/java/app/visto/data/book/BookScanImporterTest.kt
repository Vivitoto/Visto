package app.visto.data.book

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.account.ReaderDefaultSettings
import app.visto.data.db.BookProgressEntity
import app.visto.data.db.DavAccountEntity
import app.visto.data.db.VistoDatabase
import app.visto.ui.reader.ReaderPageMargins
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BookScanImporterTest {

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

    private suspend fun seedAccount(): Long {
        return db.davAccountDao().insert(
            DavAccountEntity(
                displayName = "books",
                baseUrl = "https://dav.example.com",
                rootPath = "/",
                username = "u",
                credentialRef = "ref",
                createdAt = 1,
                updatedAt = 1,
            )
        )
    }

    @Test
    fun importsNewBooksWithCurrentDefaultReaderSettings() = runBlocking {
        val accountId = seedAccount()
        val settings = ReaderDefaultSettings(
            fontSizeSp = 22,
            lineSpacing = 1.8f,
            theme = "cream",
            fontChoice = "serif",
            textColor = "ink",
            backgroundStyle = "paper",
            pageMargins = ReaderPageMargins(topDp = 20, bottomDp = 72, startDp = 24, endDp = 30),
        )

        val result = BookScanImporter.importBooks(
            accountId = accountId,
            books = listOf(book("/Books/a.txt", sizeBytes = 123, etag = "e1")),
            now = 100,
            defaultSettings = settings,
            dao = db.bookProgressDao(),
        )

        val saved = db.bookProgressDao().getByPath(accountId, "/Books/a.txt")
        assertEquals(BookScanImportResult(imported = 1, updated = 0), result)
        assertEquals("a.txt", saved?.name)
        assertEquals(123L, saved?.sizeBytes)
        assertEquals("e1", saved?.etag)
        assertEquals(22, saved?.fontSizeSp)
        assertEquals(1.8f, saved?.lineSpacing ?: 0f, 0.0001f)
        assertEquals("cream", saved?.theme)
        assertEquals("serif", saved?.fontChoice)
        assertEquals("ink", saved?.textColor)
        assertEquals("paper", saved?.backgroundStyle)
        assertEquals(20, saved?.pageMarginTopDp)
        assertEquals(72, saved?.pageMarginBottomDp)
        assertEquals(24, saved?.pageMarginStartDp)
        assertEquals(30, saved?.pageMarginEndDp)
        assertEquals(100L, saved?.lastReadAt)
        assertEquals(100L, saved?.addedAt)
    }

    @Test
    fun rescanUpdatesOnlyBookMetadataForExistingProgress() = runBlocking {
        val accountId = seedAccount()
        db.bookProgressDao().upsert(
            BookProgressEntity(
                accountId = accountId,
                path = "/Books/a.txt",
                name = "旧名.txt",
                sizeBytes = 10,
                etag = "old",
                encoding = "GBK",
                chapterIndex = 4,
                chapterTitle = "第五章",
                pageOffset = 7,
                pageStartChar = 2048,
                totalChapters = 12,
                fontSizeSp = 24,
                lineSpacing = 2.0f,
                theme = "dark",
                fontChoice = "custom-font.ttf",
                textColor = "ink",
                backgroundStyle = "paper",
                pageMarginTopDp = 30,
                pageMarginBottomDp = 80,
                pageMarginStartDp = 32,
                pageMarginEndDp = 34,
                lastReadAt = 500,
                addedAt = 400,
            )
        )

        val result = BookScanImporter.importBooks(
            accountId = accountId,
            books = listOf(book("/Books/a.txt", name = "新名.txt", sizeBytes = 99, etag = "new")),
            now = 900,
            defaultSettings = ReaderDefaultSettings(),
            dao = db.bookProgressDao(),
        )

        val saved = db.bookProgressDao().getByPath(accountId, "/Books/a.txt")
        assertEquals(BookScanImportResult(imported = 0, updated = 1), result)
        assertEquals("新名.txt", saved?.name)
        assertEquals(99L, saved?.sizeBytes)
        assertEquals("new", saved?.etag)
        assertEquals("GBK", saved?.encoding)
        assertEquals(4, saved?.chapterIndex)
        assertEquals("第五章", saved?.chapterTitle)
        assertEquals(7, saved?.pageOffset)
        assertEquals(2048, saved?.pageStartChar)
        assertEquals(12, saved?.totalChapters)
        assertEquals(24, saved?.fontSizeSp)
        assertEquals(2.0f, saved?.lineSpacing ?: 0f, 0.0001f)
        assertEquals("dark", saved?.theme)
        assertEquals("custom-font.ttf", saved?.fontChoice)
        assertEquals("ink", saved?.textColor)
        assertEquals("paper", saved?.backgroundStyle)
        assertEquals(30, saved?.pageMarginTopDp)
        assertEquals(80, saved?.pageMarginBottomDp)
        assertEquals(32, saved?.pageMarginStartDp)
        assertEquals(34, saved?.pageMarginEndDp)
        assertEquals(500L, saved?.lastReadAt)
        assertEquals(400L, saved?.addedAt)
    }

    private fun book(
        path: String,
        name: String = path.substringAfterLast('/'),
        sizeBytes: Long? = null,
        etag: String? = null,
    ): RemoteEntry = RemoteEntry(
        accountId = 1,
        parentPath = path.substringBeforeLast('/', missingDelimiterValue = "/").ifBlank { "/" },
        path = path,
        name = name,
        isDirectory = false,
        mediaType = MediaType.TEXT_BOOK,
        sizeBytes = sizeBytes,
        etag = etag,
    )
}
