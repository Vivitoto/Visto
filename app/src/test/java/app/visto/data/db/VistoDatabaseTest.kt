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
class VistoDatabaseTest {

    private lateinit var db: VistoDatabase
    private lateinit var accountDao: DavAccountDao
    private lateinit var remoteDao: RemoteEntryDao
    private lateinit var thumbDao: ThumbnailCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = db.davAccountDao()
        remoteDao = db.remoteEntryDao()
        thumbDao = db.thumbnailCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAccountAndQueryActive() = runBlocking {
        assertNull(accountDao.getActive())
        val id = accountDao.insert(
            DavAccountEntity(
                displayName = "Home",
                baseUrl = "https://dav.example.com/dav",
                rootPath = "/Photos",
                username = "alice",
                credentialRef = "ref-1",
                createdAt = 1,
                updatedAt = 1,
            )
        )
        assertTrue(id > 0)
        accountDao.markActive(id, updatedAt = 2)
        val active = accountDao.getActive()
        assertNotNull(active)
        assertEquals("Home", active!!.displayName)
        assertEquals(true, active.isActive)
    }

    @Test
    fun replaceRemoteEntriesForParent() = runBlocking {
        val accountId = accountDao.insert(
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
        remoteDao.insertAll(
            listOf(
                RemoteEntryEntity(
                    id = 0,
                    accountId = accountId,
                    parentPath = "/Photos",
                    path = "/Photos/a.jpg",
                    name = "a.jpg",
                    isDirectory = false,
                    mediaType = "IMAGE",
                    mimeType = "image/jpeg",
                    sizeBytes = 1,
                    etag = null,
                    lastModifiedEpochMs = null,
                    lastSeenAt = 100,
                    sortName = "a.jpg",
                ),
                RemoteEntryEntity(
                    id = 0,
                    accountId = accountId,
                    parentPath = "/Photos",
                    path = "/Photos/b.jpg",
                    name = "b.jpg",
                    isDirectory = false,
                    mediaType = "IMAGE",
                    mimeType = "image/jpeg",
                    sizeBytes = 1,
                    etag = null,
                    lastModifiedEpochMs = null,
                    lastSeenAt = 100,
                    sortName = "b.jpg",
                ),
            )
        )

        val before = remoteDao.entriesForParent(accountId, "/Photos")
        assertEquals(2, before.size)

        // Refresh leaves only one file
        remoteDao.deleteForParent(accountId, "/Photos")
        remoteDao.insertAll(
            listOf(
                RemoteEntryEntity(
                    id = 0,
                    accountId = accountId,
                    parentPath = "/Photos",
                    path = "/Photos/a.jpg",
                    name = "a.jpg",
                    isDirectory = false,
                    mediaType = "IMAGE",
                    mimeType = "image/jpeg",
                    sizeBytes = 1,
                    etag = "etag2",
                    lastModifiedEpochMs = null,
                    lastSeenAt = 200,
                    sortName = "a.jpg",
                ),
            )
        )
        val after = remoteDao.entriesForParent(accountId, "/Photos")
        assertEquals(1, after.size)
        assertEquals("a.jpg", after.first().name)
        assertEquals("etag2", after.first().etag)
    }

    @Test
    fun thumbnailLruQueryReturnsOldestFirst() = runBlocking {
        val accountId = accountDao.insert(
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
        val ids = remoteDao.insertAll(
            (1..3).map { i ->
                RemoteEntryEntity(
                    id = 0,
                    accountId = accountId,
                    parentPath = "/",
                    path = "/p$i.jpg",
                    name = "p$i.jpg",
                    isDirectory = false,
                    mediaType = "IMAGE",
                    mimeType = "image/jpeg",
                    sizeBytes = 1,
                    etag = null,
                    lastModifiedEpochMs = null,
                    lastSeenAt = 1,
                    sortName = "p$i.jpg",
                )
            }
        )
        ids.forEachIndexed { idx, id ->
            thumbDao.upsert(
                ThumbnailCacheEntity(
                    id = 0,
                    remoteEntryId = id,
                    cacheKey = "k$idx",
                    thumbPath = "/cache/k$idx",
                    width = 240,
                    height = 240,
                    sourceEtag = null,
                    sourceSizeBytes = 100,
                    createdAt = 100L * idx,
                    lastAccessedAt = 100L * idx,
                    bytesOnDisk = 1024,
                    status = "ready",
                )
            )
        }
        val ordered = thumbDao.readyByOldestAccess()
        assertEquals(listOf("k0", "k1", "k2"), ordered.map { it.cacheKey })
        assertEquals(3 * 1024L, thumbDao.totalBytesOnDisk())
    }
}
