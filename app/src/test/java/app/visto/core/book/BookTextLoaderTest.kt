package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
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
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/小说.txt", cacheDir)

        assertEquals("第一章 开始\n你好 Visto", result.text)
        assertEquals("UTF-8", result.encoding)
        assertEquals("第一章 开始\n你好 Visto".toByteArray(Charsets.UTF_8).size.toLong(), result.sizeBytes)
        assertTrue(result.cachedFile.exists())
        assertEquals("books", result.cachedFile.parentFile?.name)
        assertEquals("第一章 开始\n你好 Visto", result.cachedFile.readText(Charsets.UTF_8))
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun loadDetectsAndDecodesGbkText() = runBlocking {
        val gbkBytes = "第二章 中文".toByteArray(Charset.forName("GBK"))
        server.enqueue(MockResponse().setBody(Buffer().write(gbkBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/gbk.txt", cacheDir)

        assertEquals("GBK", result.encoding)
        assertEquals("第二章 中文", result.text)
        assertEquals(gbkBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun cachedFileWithMatchingExpectedEtagSkipsDownload() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "same"))
        BookTextLoader.load(client(), "/books/a.txt", cacheDir)
        assertEquals(1, server.requestCount)

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("旧内容", result.text)
        assertEquals("UTF-8", result.encoding)
        assertEquals(1, server.requestCount)
    }

    private fun client(): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(
            baseUrl = server.url("/dav").toString(),
            username = "alice",
            password = "secret",
        ),
        accountId = 1L,
    )
}
