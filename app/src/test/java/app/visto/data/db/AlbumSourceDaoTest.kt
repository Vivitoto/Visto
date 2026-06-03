package app.visto.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AlbumSourceDaoTest {

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
                username = "u",
                credentialRef = "ref-$name",
                createdAt = 1,
                updatedAt = 1,
            )
        )
    }

    @Test
    fun insertListAndCount() = runBlocking {
        val acc = seedAccount()
        val id1 = db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "家庭", rootPath = "/Photos/Family", createdAt = 1, updatedAt = 1)
        )
        val id2 = db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "旅行", rootPath = "/Photos/Travel", createdAt = 2, updatedAt = 2)
        )
        val all = db.albumSourceDao().listForAccount(acc)
        assertEquals(listOf(id1, id2), all.map { it.id })
        assertEquals(2, db.albumSourceDao().count())
        assertEquals("家庭", db.albumSourceDao().findById(id1)?.displayName)
    }

    @Test
    fun uniqueByAccountAndRootPath() = runBlocking {
        val acc = seedAccount()
        db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "家庭", rootPath = "/Photos/Family", createdAt = 1, updatedAt = 1)
        )
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            runBlocking {
                db.albumSourceDao().insert(
                    AlbumSourceEntity(accountId = acc, displayName = "家庭 2", rootPath = "/Photos/Family", createdAt = 2, updatedAt = 2)
                )
            }
        }
    }

    @Test
    fun deleteByIdRemovesRow() = runBlocking {
        val acc = seedAccount()
        val id = db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "A", rootPath = "/A", createdAt = 1, updatedAt = 1)
        )
        db.albumSourceDao().deleteById(id)
        assertNull(db.albumSourceDao().findById(id))
    }

    @Test
    fun cascadeOnAccountDelete() = runBlocking {
        val acc = seedAccount()
        db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "X", rootPath = "/X", createdAt = 1, updatedAt = 1)
        )
        db.davAccountDao().deleteById(acc)
        assertEquals(emptyList<AlbumSourceEntity>(), db.albumSourceDao().listForAccount(acc))
        assertEquals(0, db.albumSourceDao().count())
    }

    @Test
    fun renameUpdatesDisplayName() = runBlocking {
        val acc = seedAccount()
        val id = db.albumSourceDao().insert(
            AlbumSourceEntity(accountId = acc, displayName = "旧", rootPath = "/A", createdAt = 1, updatedAt = 1)
        )
        db.albumSourceDao().renameById(id, "新", 99)
        val row = db.albumSourceDao().findById(id)
        assertNotNull(row)
        assertEquals("新", row!!.displayName)
        assertEquals(99, row.updatedAt)
    }
}
