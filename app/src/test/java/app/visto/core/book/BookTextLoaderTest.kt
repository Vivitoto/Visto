package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.Charset

class BookTextLoaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loadDownloadsUtf8TextAndCreatesCache() = runBlocking {
        server.enqueue(MockResponse().setBody("第一章 开始\n你好 Visto").setHeader("ETag", "book-v1"))

        val result = loader().load("/books/小说.txt")

        assertEquals("第一章 开始\n你好 Visto", result.text)
        assertEquals("UTF-8", result.encoding)
        assertEquals("第一章 开始\n你好 Visto".toByteArray(Charsets.UTF_8).size.toLong(), result.sizeBytes)
        assertTrue(result.cachedFile.exists())
        assertEquals("第一章 开始\n你好 Visto", result.cachedFile.readText(Charsets.UTF_8))
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun loadDetectsAndDecodesGbkText() = runBlocking {
        val gbkBytes = "第二章 中文".toByteArray(Charset.forName("GBK"))
        server.enqueue(MockResponse().setBody(okio.Buffer().write(gbkBytes)))

        val result = loader().load("/books/gbk.txt")

        assertEquals("GBK", result.encoding)
        assertEquals("第二章 中文", result.text)
        assertEquals(gbkBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun cachedFileWithUnchangedEtagSkipsGet() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        val bookLoader = loader(cacheDir)
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "same"))
        bookLoader.load("/books/a.txt")
        server.takeRequest()

        server.enqueue(MockResponse().setHeader("ETag", "same").setHeader("Content-Length", "9"))
        val result = bookLoader.load("/books/a.txt", knownEtag = "same")

        assertEquals("旧内容", result.text)
        assertEquals("HEAD", server.takeRequest().method)
    }

    @Test
    fun cachedFileWithChangedEtagDownloadsAgain() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        val bookLoader = loader(cacheDir)
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "old"))
        bookLoader.load("/books/a.txt")
        server.takeRequest()

        server.enqueue(MockResponse().setHeader("ETag", "new"))
        server.enqueue(MockResponse().setBody("新内容").setHeader("ETag", "new"))
        val result = bookLoader.load("/books/a.txt", knownEtag = "old")

        assertEquals("新内容", result.text)
        assertEquals("HEAD", server.takeRequest().method)
        assertEquals("GET", server.takeRequest().method)
        assertEquals("新内容", result.cachedFile.readText(Charsets.UTF_8))
    }

    private fun loader(cacheDir: java.io.File = temporaryFolder.newFolder("cache")): BookTextLoader =
        BookTextLoader(webDavClient = client(), cacheDir = cacheDir)

    private fun client(): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(
            baseUrl = server.url("/dav").toString(),
            username = "alice",
            password = "secret",
        ),
        accountId = 1L,
    )
}
