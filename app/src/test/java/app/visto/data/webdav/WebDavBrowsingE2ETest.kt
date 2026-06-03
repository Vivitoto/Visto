package app.visto.data.webdav

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.db.VistoDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WebDavBrowsingE2ETest {

    private lateinit var server: MockWebServer
    private lateinit var db: VistoDatabase
    private lateinit var repo: RemoteEntryRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, VistoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RemoteEntryRepository(db, db.remoteEntryDao())
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun listDirectoryAndCacheRoundTrip() = runBlocking {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Photos/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Photos/Trip/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:displayname>Trip</d:displayname>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Photos/a.jpg</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype/>
                    <d:getcontenttype>image/jpeg</d:getcontenttype>
                    <d:getcontentlength>4321</d:getcontentlength>
                    <d:getetag>"e1"</d:getetag>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(207).setBody(xml))

        val accountId = 7L
        val client = WebDavClient(
            credentials = WebDavCredentials(
                baseUrl = server.url("/dav").toString(),
                username = "alice",
                password = "secret",
            ),
            accountId = accountId,
        )

        val entries = client.listDirectory("/Photos")
        repo.replaceDirectoryListing(accountId, "/Photos", entries)

        val cached = repo.entriesForParent(accountId, "/Photos")
        assertEquals(listOf("Trip", "a.jpg"), cached.map { it.name }.sorted())
        val photo = cached.first { it.name == "a.jpg" }
        assertEquals("image/jpeg", photo.mimeType)
        assertEquals(4321L, photo.sizeBytes)
        assertEquals("\"e1\"", photo.etag)
    }
}
