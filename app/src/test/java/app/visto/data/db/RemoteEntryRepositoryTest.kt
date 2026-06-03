package app.visto.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
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
class RemoteEntryRepositoryTest {

    private lateinit var db: VistoDatabase
    private lateinit var repo: RemoteEntryRepository
    private var accountId: Long = 0

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountId = db.davAccountDao().insert(
            DavAccountEntity(
                displayName = "A",
                baseUrl = "https://x",
                rootPath = "/",
                username = "u",
                credentialRef = "r",
                createdAt = 1,
                updatedAt = 1,
            )
        )
        var now = 0L
        repo = RemoteEntryRepository(
            database = db,
            dao = db.remoteEntryDao(),
            clock = { now++ },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun image(name: String, sizeBytes: Long = 100, modified: Long? = null): RemoteEntry =
        RemoteEntry(
            accountId = accountId,
            parentPath = "/Photos",
            path = "/Photos/$name",
            name = name,
            isDirectory = false,
            mediaType = MediaType.IMAGE,
            mimeType = "image/jpeg",
            sizeBytes = sizeBytes,
            etag = null,
            lastModifiedEpochMs = modified,
        )

    @Test
    fun replaceDirectoryReplacesEntriesAtomically() = runBlocking {
        repo.replaceDirectoryListing(
            accountId = accountId,
            parentPath = "/Photos",
            entries = listOf(image("a.jpg"), image("b.jpg")),
        )
        val before = repo.entriesForParent(accountId, "/Photos")
        assertEquals(listOf("a.jpg", "b.jpg"), before.map { it.name }.sorted())

        repo.replaceDirectoryListing(
            accountId = accountId,
            parentPath = "/Photos",
            entries = listOf(image("a.jpg")),
        )
        val after = repo.entriesForParent(accountId, "/Photos")
        assertEquals(listOf("a.jpg"), after.map { it.name })
    }

    @Test
    fun replaceDirectoryClearsToEmpty() = runBlocking {
        repo.replaceDirectoryListing(
            accountId = accountId,
            parentPath = "/Photos",
            entries = listOf(image("a.jpg")),
        )
        repo.replaceDirectoryListing(
            accountId = accountId,
            parentPath = "/Photos",
            entries = emptyList(),
        )
        assertEquals(0, repo.entriesForParent(accountId, "/Photos").size)
    }

    @Test
    fun entryByPathFindsCachedEntry() = runBlocking {
        repo.replaceDirectoryListing(
            accountId = accountId,
            parentPath = "/Photos",
            entries = listOf(image("a.jpg")),
        )
        val found = repo.entryByPath(accountId, "/Photos/a.jpg")
        assertEquals("a.jpg", found?.name)
        assertEquals(MediaType.IMAGE, found?.mediaType)
    }
}
