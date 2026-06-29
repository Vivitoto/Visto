package app.visto.core.book

import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
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

        assertEquals("第一章 开始\n\u3000\u3000你好 Visto", result.text)
        assertEquals("UTF-8", result.encoding)
        assertEquals("第一章 开始\n你好 Visto".toByteArray(Charsets.UTF_8).size.toLong(), result.sizeBytes)
        assertEquals("book-v1", result.etag)
        assertTrue(result.cachedFile.exists())
        assertEquals("1", result.cachedFile.parentFile?.name)
        assertEquals("books", result.cachedFile.parentFile?.parentFile?.name)
        assertEquals("第一章 开始\n\u3000\u3000你好 Visto", result.cachedFile.readText(Charsets.UTF_8))
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
    fun loadDetectsAndDecodesGbkTextAfterLongAsciiPrefix() = runBlocking {
        val rawText = "a".repeat(5_000) + "\n" + "第二章 中文内容".repeat(24)
        val gbkBytes = rawText.toByteArray(Charset.forName("GBK"))
        server.enqueue(MockResponse().setBody(Buffer().write(gbkBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/gbk-preface.txt", cacheDir)

        assertEquals("GBK", result.encoding)
        assertTrue(result.text.contains("第二章 中文内容"))
        assertFalse(result.text.contains('\uFFFD'))
        assertEquals(gbkBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun loadDetectsAndDecodesBig5Text() = runBlocking {
        val rawText = "繁體中文測試 國語閱讀"
        val big5Bytes = rawText.toByteArray(Charset.forName("Big5"))
        server.enqueue(MockResponse().setBody(Buffer().write(big5Bytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/big5.txt", cacheDir)

        assertEquals("Big5", result.encoding)
        assertTrue(result.text.contains(rawText))
        assertFalse(result.text.contains('\uFFFD'))
        assertEquals(big5Bytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun loadDetectsAndDecodesEucJpText() = runBlocking {
        val rawText = "第一章 日本語の文章です。漢字とかな。"
        val eucJpBytes = rawText.toByteArray(Charset.forName("EUC-JP"))
        server.enqueue(MockResponse().setBody(Buffer().write(eucJpBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/euc-jp.txt", cacheDir)

        assertEquals("EUC-JP", result.encoding)
        assertTrue(result.text.contains("日本語の文章です"))
        assertFalse(result.text.contains('\uFFFD'))
        assertEquals(eucJpBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun loadDetectsAndDecodesShiftJisText() = runBlocking {
        val rawText = "第一章 日本語の文章です。漢字とカナ。"
        val shiftJisBytes = rawText.toByteArray(Charset.forName("Shift_JIS"))
        server.enqueue(MockResponse().setBody(Buffer().write(shiftJisBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/shift-jis.txt", cacheDir)

        assertEquals("Shift_JIS", result.encoding)
        assertTrue(result.text.contains("日本語の文章です"))
        assertFalse(result.text.contains('\uFFFD'))
        assertEquals(shiftJisBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun loadDetectsAndDecodesEucKrText() = runBlocking {
        val rawText = "첫 장 한국어 문장입니다. 한글 내용입니다."
        val eucKrBytes = rawText.toByteArray(Charset.forName("EUC-KR"))
        server.enqueue(MockResponse().setBody(Buffer().write(eucKrBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/euc-kr.txt", cacheDir)

        assertEquals("EUC-KR", result.encoding)
        assertTrue(result.text.contains("한국어 문장입니다"))
        assertFalse(result.text.contains('\uFFFD'))
        assertEquals(eucKrBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun loadLenientlyDecodesGbkTextWithMinorCorruption() = runBlocking {
        val rawText = "第二章 中文内容 ".repeat(12)
        val corruptBytes = mutableListOf<Byte>()
        var insertedCorruptBytes = 0
        for (byte in rawText.toByteArray(Charset.forName("GBK"))) {
            if (insertedCorruptBytes < 3 && byte == 0x20.toByte()) {
                corruptBytes += 0x81.toByte()
                insertedCorruptBytes += 1
            }
            corruptBytes += byte
        }
        val gbkBytes = corruptBytes.toByteArray()
        server.enqueue(MockResponse().setBody(Buffer().write(gbkBytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val result = BookTextLoader.load(client(), "/books/corrupt-gbk.txt", cacheDir)

        assertEquals("GBK", result.encoding)
        assertTrue(result.text.contains("第二章"))
        assertTrue(result.text.contains("中文内容"))
        assertTrue(result.text.count { it == '\uFFFD' } in 1..3)
        assertEquals(gbkBytes.size.toLong(), result.sizeBytes)
    }

    @Test
    fun binaryLikeDownloadedTextIsRejected() {
        val bytes = ByteArray(64) { index -> if (index % 4 == 0) 0 else (index + 1).toByte() }
        server.enqueue(MockResponse().setBody(Buffer().write(bytes)))
        val cacheDir = temporaryFolder.newFolder("cache")

        val error = assertThrows(IOException::class.java) {
            runBlocking {
                BookTextLoader.load(client(), "/books/not-text.bin", cacheDir)
            }
        }

        assertTrue(error.message.orEmpty().contains("unreadable control characters"))
    }

    @Test
    fun cachedFileWithMatchingExpectedEtagSkipsDownload() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "same"))
        server.enqueue(MockResponse().setBody("不应下载"))
        BookTextLoader.load(client(), "/books/a.txt", cacheDir)
        assertEquals(1, server.requestCount)

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("\u3000\u3000旧内容", result.text)
        assertEquals("UTF-8", result.encoding)
        assertEquals("same", result.etag)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cachedFileWithoutExpectedEtagSkipsDownload() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("缓存内容").setHeader("ETag", "cached"))
        server.enqueue(MockResponse().setBody("网络内容"))
        BookTextLoader.load(client(), "/books/a.txt", cacheDir)

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir)

        assertEquals("\u3000\u3000缓存内容", result.text)
        assertEquals("cached", result.etag)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun oldCachedFileIsRefreshedFromRawDownloadWhenPossible() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("原始内容").setHeader("ETag", "same"))
        server.enqueue(MockResponse().setBody("硬换行第一句，\n第二句。").setHeader("ETag", "same"))
        val first = BookTextLoader.load(client(), "/books/a.txt", cacheDir)
        val metaFile = java.io.File(first.cachedFile.parentFile, "${first.cachedFile.name}.meta")
        first.cachedFile.writeText("\u3000\u3000旧缓存第一句，\n\u3000\u3000第二句。", Charsets.UTF_8)
        metaFile.writeText(
            """
            etag=same
            encoding=UTF-8
            sizeBytes=18
            normalizerVersion=1
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("\u3000\u3000硬换行第一句，第二句。", result.text)
        assertEquals(result.text, first.cachedFile.readText(Charsets.UTF_8))
        assertTrue(metaFile.readText(Charsets.UTF_8).contains("normalizerVersion=3"))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun malformedUtf8CacheIsInvalidatedAndDownloadedAgain() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "same"))
        server.enqueue(MockResponse().setBody("新内容").setHeader("ETag", "same"))
        val first = BookTextLoader.load(client(), "/books/a.txt", cacheDir)
        first.cachedFile.writeBytes(byteArrayOf(0x48, 0x65, 0x6c, 0xc3.toByte(), 0x28))

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("\u3000\u3000新内容", result.text)
        assertEquals(2, server.requestCount)
        assertEquals("\u3000\u3000新内容", first.cachedFile.readText(Charsets.UTF_8))
    }

    @Test
    fun emptyCacheIsInvalidatedAndDownloadedAgain() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("旧内容").setHeader("ETag", "same"))
        server.enqueue(MockResponse().setBody("新内容").setHeader("ETag", "same"))
        val first = BookTextLoader.load(client(), "/books/a.txt", cacheDir)
        first.cachedFile.writeBytes(byteArrayOf())

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("\u3000\u3000新内容", result.text)
        assertEquals(2, server.requestCount)
        assertEquals("\u3000\u3000新内容", first.cachedFile.readText(Charsets.UTF_8))
    }

    @Test
    fun staleCacheFallsBackWhenRefreshFails() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("旧缓存").setHeader("ETag", "old"))
        server.enqueue(MockResponse().setResponseCode(500))
        BookTextLoader.load(client(), "/books/a.txt", cacheDir)

        val result = BookTextLoader.load(client(), "/books/a.txt", cacheDir, expectedEtag = "new")

        assertEquals("\u3000\u3000旧缓存", result.text)
        assertEquals("old", result.etag)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun cacheIsScopedByAccountId() = runBlocking {
        val cacheDir = temporaryFolder.newFolder("cache")
        server.enqueue(MockResponse().setBody("账号一").setHeader("ETag", "same"))
        server.enqueue(MockResponse().setBody("账号二").setHeader("ETag", "same"))

        BookTextLoader.load(client(accountId = 1L), "/books/a.txt", cacheDir)
        val result = BookTextLoader.load(client(accountId = 2L), "/books/a.txt", cacheDir, expectedEtag = "same")

        assertEquals("\u3000\u3000账号二", result.text)
        assertEquals(2, server.requestCount)
    }

    private fun client(accountId: Long = 1L): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(
            baseUrl = server.url("/dav").toString(),
            username = "alice",
            password = "secret",
        ),
        accountId = accountId,
    )
}
