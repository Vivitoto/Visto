package app.visto.data.album

import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlbumLoaderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun multistatus(self: String, children: List<Pair<String, Boolean>>): String {
        val buf = StringBuilder()
        buf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        buf.append("<d:multistatus xmlns:d=\"DAV:\">")
        buf.append(responseBlock(self, isDir = true, mime = null))
        for ((href, isDir) in children) {
            buf.append(responseBlock(href, isDir = isDir, mime = if (isDir) null else "image/jpeg"))
        }
        buf.append("</d:multistatus>")
        return buf.toString()
    }

    private fun responseBlock(href: String, isDir: Boolean, mime: String?): String {
        val typeXml = if (isDir) "<d:resourcetype><d:collection/></d:resourcetype>" else "<d:resourcetype/>"
        val mimeXml = if (!isDir && mime != null) "<d:getcontenttype>$mime</d:getcontenttype>" else ""
        return """
            <d:response>
              <d:href>$href</d:href>
              <d:propstat>
                <d:prop>
                  $typeXml
                  $mimeXml
                </d:prop>
                <d:status>HTTP/1.1 200 OK</d:status>
              </d:propstat>
            </d:response>
        """.trimIndent()
    }

    private fun setRoutes(routes: Map<String, MockResponse>) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val key = request.path?.removePrefix("/dav") ?: "/"
                return routes[key]
                    ?: routes[key.trimEnd('/')]
                    ?: routes["${key.trimEnd('/')}/"]
                    ?: MockResponse().setResponseCode(404)
            }
        }
    }

    @Test
    fun recursivelyCollectsAndGroupsByFolder() = runBlocking {
        val routes = mapOf(
            "/Photos" to MockResponse().setResponseCode(207).setBody(
                multistatus(
                    self = "/dav/Photos/",
                    children = listOf(
                        "/dav/Photos/a.jpg" to false,
                        "/dav/Photos/2024/" to true,
                    ),
                )
            ),
            "/Photos/2024" to MockResponse().setResponseCode(207).setBody(
                multistatus(
                    self = "/dav/Photos/2024/",
                    children = listOf(
                        "/dav/Photos/2024/m.jpg" to false,
                        "/dav/Photos/2024/Trip/" to true,
                    ),
                )
            ),
            "/Photos/2024/Trip" to MockResponse().setResponseCode(207).setBody(
                multistatus(
                    self = "/dav/Photos/2024/Trip/",
                    children = listOf(
                        "/dav/Photos/2024/Trip/z.jpg" to false,
                    ),
                )
            ),
        )
        setRoutes(routes)

        val client = WebDavClient(
            credentials = WebDavCredentials(
                baseUrl = server.url("/dav").toString(),
                username = "u",
                password = "p",
            ),
            accountId = 1L,
        )
        val loader = AlbumLoader(client)
        val contents = loader.loadCollected("/Photos")
        assertEquals(3, contents.totalMedia)
        assertEquals(listOf("", "2024", "2024/Trip"), contents.sections.map { it.title })
        assertEquals(0, contents.foldersFailed)
        assertEquals(3, contents.foldersVisited)
    }

    @Test
    fun depthCapStopsRecursion() = runBlocking {
        val routes = mapOf(
            "/Root" to MockResponse().setResponseCode(207).setBody(
                multistatus(self = "/dav/Root/", children = listOf("/dav/Root/Sub/" to true))
            ),
            "/Root/Sub" to MockResponse().setResponseCode(207).setBody(
                multistatus(self = "/dav/Root/Sub/", children = listOf("/dav/Root/Sub/Inner/" to true))
            ),
            "/Root/Sub/Inner" to MockResponse().setResponseCode(207).setBody(
                multistatus(self = "/dav/Root/Sub/Inner/", children = listOf("/dav/Root/Sub/Inner/x.jpg" to false))
            ),
        )
        setRoutes(routes)

        val client = WebDavClient(
            credentials = WebDavCredentials(server.url("/dav").toString(), "u", "p"),
            accountId = 1L,
        )
        // Cap at 1 -> visits /Root and /Root/Sub, never /Root/Sub/Inner, so 0 media.
        val loader = AlbumLoader(client, maxDepth = 1)
        val contents = loader.loadCollected("/Root")
        assertEquals(0, contents.totalMedia)
        assertEquals(2, contents.foldersVisited)
    }

    @Test
    fun singleFolderFailureDoesNotKillTraversal() = runBlocking {
        val routes = mapOf(
            "/Mix" to MockResponse().setResponseCode(207).setBody(
                multistatus(
                    self = "/dav/Mix/",
                    children = listOf(
                        "/dav/Mix/Good/" to true,
                        "/dav/Mix/Bad/" to true,
                    ),
                )
            ),
            "/Mix/Good" to MockResponse().setResponseCode(207).setBody(
                multistatus(self = "/dav/Mix/Good/", children = listOf("/dav/Mix/Good/g.jpg" to false))
            ),
            "/Mix/Bad" to MockResponse().setResponseCode(500).setBody("boom"),
        )
        setRoutes(routes)

        val client = WebDavClient(
            credentials = WebDavCredentials(server.url("/dav").toString(), "u", "p"),
            accountId = 1L,
        )
        val contents = AlbumLoader(client).loadCollected("/Mix")
        assertEquals(1, contents.totalMedia)
        assertEquals(1, contents.foldersFailed)
        assertTrue(contents.warnings.any { it.contains("/Mix/Bad") })
    }
}
