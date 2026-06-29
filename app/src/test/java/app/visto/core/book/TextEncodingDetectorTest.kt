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
    fun gbkEncodedChineseTextAfterLongAsciiPrefixIsDetectedAsGbk() {
        val text = "a".repeat(5_000) + "第二章 中文内容".repeat(24)
        val bytes = text.toByteArray(Charset.forName("GBK"))

        assertEquals("GBK", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun big5EncodedTraditionalChineseTextIsDetectedAsBig5() {
        val bytes = "繁體中文測試 國語閱讀".toByteArray(Charset.forName("Big5"))

        assertEquals("Big5", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun eucJpEncodedJapaneseTextIsDetectedAsEucJp() {
        val bytes = "第一章 日本語の文章です。漢字とかな。".toByteArray(Charset.forName("EUC-JP"))

        assertEquals("EUC-JP", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun eucKrEncodedKoreanTextIsDetectedAsEucKr() {
        val bytes = "첫 장 한국어 문장입니다. 한글 내용입니다.".toByteArray(Charset.forName("EUC-KR"))

        assertEquals("EUC-KR", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun shiftJisEncodedJapaneseTextIsDetectedAsShiftJis() {
        val bytes = "第一章 日本語の文章です。漢字とカナ。".toByteArray(Charset.forName("Shift_JIS"))

        assertEquals("Shift_JIS", TextEncodingDetector.detect(bytes))
    }

    @Test
    fun gb18030EncodedChineseTextIsDetectedAsGb18030() {
        val text = "第一章 \uD840\uDC00 \uD840\uDC01 中文内容"
        val bytes = text.toByteArray(Charset.forName("GB18030"))

        assertEquals("GB18030", TextEncodingDetector.detect(bytes))
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
