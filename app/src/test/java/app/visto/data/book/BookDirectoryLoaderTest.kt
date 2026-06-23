package app.visto.data.book

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookDirectoryLoaderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun scannerAcceptsOnlyTextAndEpubFiles() {
        assertTrue(BookDirectoryScanner.isBookEntry(entry("a.txt", MediaType.TEXT_BOOK)))
        assertTrue(BookDirectoryScanner.isBookEntry(entry("a.epub", MediaType.EPUB_BOOK)))
        assertFalse(BookDirectoryScanner.isBookEntry(entry("a.jpg", MediaType.IMAGE)))
        assertFalse(BookDirectoryScanner.isBookEntry(entry("Books", MediaType.OTHER, isDirectory = true)))
    }

    @Test
    fun recursivelyCollectsTextMarkdownAndEpubBooks() = runBlocking {
        setRoutes(
            mapOf(
                "/Books" to MockResponse().setResponseCode(207).setBody(
                    multistatus(
                        self = "/dav/Books/",
                        children = listOf(
                            Child("/dav/Books/a.txt", isDir = false, mime = "text/plain"),
                            Child("/dav/Books/README.md", isDir = false, mime = null),
                            Child("/dav/Books/photo.jpg", isDir = false, mime = "image/jpeg"),
                            Child("/dav/Books/Sub/", isDir = true),
                        ),
                    )
                ),
                "/Books/Sub" to MockResponse().setResponseCode(207).setBody(
                    multistatus(
                        self = "/dav/Books/Sub/",
                        children = listOf(
                            Child("/dav/Books/Sub/story.epub", isDir = false, mime = "application/epub+zip"),
                            Child("/dav/Books/Sub/archive.pdf", isDir = false, mime = "application/pdf"),
                        ),
                    )
                ),
            )
        )

        val result = BookDirectoryLoader(client()).loadCollected("/Books")

        assertEquals("/Books", result.rootPath)
        assertEquals(
            listOf("/Books/a.txt", "/Books/README.md", "/Books/Sub/story.epub"),
            result.books.map { it.path },
        )
        assertEquals(2, result.foldersVisited)
        assertEquals(0, result.foldersFailed)
    }

    @Test
    fun singleFolderFailureDoesNotStopSiblingFolders() = runBlocking {
        setRoutes(
            mapOf(
                "/Mix" to MockResponse().setResponseCode(207).setBody(
                    multistatus(
                        self = "/dav/Mix/",
                        children = listOf(
                            Child("/dav/Mix/Good/", isDir = true),
                            Child("/dav/Mix/Bad/", isDir = true),
                        ),
                    )
                ),
                "/Mix/Good" to MockResponse().setResponseCode(207).setBody(
                    multistatus(
                        self = "/dav/Mix/Good/",
                        children = listOf(Child("/dav/Mix/Good/g.txt", isDir = false, mime = "text/plain")),
                    )
                ),
                "/Mix/Bad" to MockResponse().setResponseCode(500).setBody("boom"),
            )
        )

        val result = BookDirectoryLoader(client()).loadCollected("/Mix")

        assertEquals(listOf("/Mix/Good/g.txt"), result.books.map { it.path })
        assertEquals(1, result.foldersFailed)
        assertTrue(result.warnings.any { it.contains("/Mix/Bad") })
    }

    private fun entry(
        name: String,
        mediaType: MediaType,
        isDirectory: Boolean = false,
    ): RemoteEntry = RemoteEntry(
        accountId = 1L,
        parentPath = "/",
        path = "/$name",
        name = name,
        isDirectory = isDirectory,
        mediaType = mediaType,
    )

    private fun client(): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(server.url("/dav").toString(), "u", "p"),
        accountId = 1L,
    )

    private data class Child(
        val href: String,
        val isDir: Boolean,
        val mime: String? = null,
    )

    private fun multistatus(self: String, children: List<Child>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        append("<d:multistatus xmlns:d=\"DAV:\">")
        append(responseBlock(self, isDir = true, mime = null))
        for (child in children) {
            append(responseBlock(child.href, child.isDir, child.mime))
        }
        append("</d:multistatus>")
    }

    private fun responseBlock(href: String, isDir: Boolean, mime: String?): String {
        val typeXml = if (isDir) "<d:resourcetype><d:collection/></d:resourcetype>" else "<d:resourcetype/>"
        val mimeXml = if (!isDir && mime != null) "<d:getcontenttype>$mime</d:getcontenttype>" else ""
        val sizeXml = if (!isDir) "<d:getcontentlength>123</d:getcontentlength>" else ""
        val etagXml = if (!isDir) "<d:getetag>etag-${href.substringAfterLast('/')}</d:getetag>" else ""
        return """
            <d:response>
              <d:href>$href</d:href>
              <d:propstat>
                <d:prop>
                  $typeXml
                  $mimeXml
                  $sizeXml
                  $etagXml
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
}
