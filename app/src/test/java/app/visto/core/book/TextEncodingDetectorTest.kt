package app.visto.core.book

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class TextEncodingDetectorTest {

    @Test
    fun utf8BomBytesAreDetectedAsUtf8() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "hello".toByteArray()

        assertEquals("UTF-8", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun gbkEncodedChineseTextIsDetectedAsGbk() {
        val bytes = "你好，世界".toByteArray(Charset.forName("GBK"))

        assertEquals("GBK", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun utf8EncodedChineseTextIsDetectedAsUtf8() {
        val bytes = "你好，世界".toByteArray(Charsets.UTF_8)

        assertEquals("UTF-8", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun asciiOnlyTextDefaultsToUtf8() {
        val bytes = "plain ascii text".toByteArray(Charsets.US_ASCII)

        assertEquals("UTF-8", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun emptyBytesDefaultToUtf8() {
        assertEquals("UTF-8", TextEncodingDetector.detect(ByteArray(0)))
    }
}
