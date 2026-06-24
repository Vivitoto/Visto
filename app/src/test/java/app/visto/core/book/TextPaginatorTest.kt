package app.visto.core.book

import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TextPaginatorTest {

    @Test
    fun emptyTextReturnsOneEmptyPage() {
        val pages = TextPaginator.paginate(
            text = "",
            maxWidthPx = 320f,
            maxHeightPx = 480f,
            fontSizeSp = 18f,
            lineSpacing = 1.5f,
            density = 1f,
        )

        assertEquals(1, pages.size)
        assertEquals(Page(startChar = 0, endChar = 0, text = ""), pages.single())
    }

    @Test
    fun shortTextFittingOneLineReturnsOnePage() {
        val text = "short line"

        val pages = TextPaginator.paginate(text, 1000f, 400f, 16f, 1.2f, 1f)

        assertEquals(1, pages.size)
        assertEquals(text, pages.single().text)
        assertEquals(0, pages.single().startChar)
        assertEquals(text.length, pages.single().endChar)
    }

    @Test
    fun longerTextReturnsMultiplePagesWithoutOverlappingCharacters() {
        val text = (1..80).joinToString(separator = "\n") { "第${it}行正文" }

        val pages = TextPaginator.paginate(text, 120f, 1f, 18f, 1.5f, 1f)

        assertTrue(pages.size > 1)
        assertEquals(0, pages.first().startChar)
        assertEquals(text.length, pages.last().endChar)
        pages.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endChar, next.startChar)
        }
        assertEquals(text, pages.joinToString(separator = "") { it.text })
    }

    @Test
    fun lineCapacityMatchesConfiguredComposeLineHeight() {
        val text = (1..4).joinToString(separator = "\n") { "Line $it" }

        val pages = TextPaginator.paginate(text, 1000f, 54f, 18f, 1.5f, 1f)

        assertEquals(2, pages.size)
        assertEquals("Line 1\nLine 2\n", pages[0].text)
        assertEquals("Line 3\nLine 4", pages[1].text)
    }

    @Test
    fun largerFontSizeCreatesMorePages() {
        val text = (1..80).joinToString(separator = "\n") { "Line $it" }

        val smallFontPages = TextPaginator.paginate(text, 180f, 160f, 14f, 1.2f, 1f)
        val largeFontPages = TextPaginator.paginate(text, 180f, 160f, 28f, 1.2f, 1f)

        assertTrue(smallFontPages.isNotEmpty())
        assertTrue(largeFontPages.isNotEmpty())
        assertEquals(text, smallFontPages.joinToString(separator = "") { it.text })
        assertEquals(text, largeFontPages.joinToString(separator = "") { it.text })
    }

    @Test
    fun largerLineSpacingCreatesMorePages() {
        val text = (1..80).joinToString(separator = "\n") { "Line $it" }

        val compactPages = TextPaginator.paginate(text, 180f, 160f, 18f, 1.0f, 1f)
        val spaciousPages = TextPaginator.paginate(text, 180f, 160f, 18f, 2.0f, 1f)

        assertTrue(compactPages.isNotEmpty())
        assertTrue(spaciousPages.isNotEmpty())
        assertEquals(text, compactPages.joinToString(separator = "") { it.text })
        assertEquals(text, spaciousPages.joinToString(separator = "") { it.text })
    }

    @Test
    fun chineseParagraphAvoidsWeakCommaPageEndAndTinyTail() {
        val paragraph = "\u3000\u3000他要去寻找家人，因为他清楚的记得，他当初用一根绳子，将自己，老妈，老姐，还有小妹都绑在了一个救生圈上面。"
        val text = List(4) { paragraph }.joinToString(separator = "\n")

        val pages = TextPaginator.paginate(text, 220f, 81f, 18f, 1.5f, 1f)

        assertTrue(pages.size > 1)
        assertEquals(text, pages.joinToString(separator = "") { it.text })
        assertFalse(pages.any { it.text.trimEnd().endsWith("老姐，") })
        assertFalse(pages.any { it.text == "还有小妹都绑在了一个救生圈上面。" })
    }

    @Test
    fun weakCommaPageEndBacktracksWithoutCreatingOneLinePage() {
        val paragraph = "\u3000\u3000他要去寻找家人，因为他清楚的记得，他当初用一根绳子，将自己，老妈，老姐，还有小妹都绑在了一个救生圈上面。"
        val weakBreak = paragraph.indexOf("还有小妹")
        val lineStarts = intArrayOf(
            0,
            paragraph.indexOf("因为"),
            paragraph.indexOf("老妈"),
            weakBreak,
            paragraph.length,
        )
        val lineEnds = intArrayOf(
            lineStarts[1],
            lineStarts[2],
            weakBreak,
            paragraph.length,
            paragraph.length,
        )

        val adjusted = TextPaginator.adjustedPageEndLineForTest(
            text = paragraph,
            lineStarts = lineStarts,
            lineEnds = lineEnds,
            startLine = 0,
            naturalEndLine = 3,
        )

        assertEquals(2, adjusted)
    }

    @Test
    fun typefaceAwarePaginationKeepsPagesContiguous() {
        val text = "\u3000\u3000" + "WiMi字形宽度不同会影响分页。".repeat(20)

        val pages = TextPaginator.paginate(text, 220f, 120f, 18f, 1.5f, 1f, typeface = Typeface.SERIF)

        assertTrue(pages.isNotEmpty())
        pages.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endChar, next.startChar)
        }
        assertEquals(text, pages.joinToString(separator = "") { it.text })
    }
}
