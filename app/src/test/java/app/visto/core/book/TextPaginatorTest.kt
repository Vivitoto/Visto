package app.visto.core.book

import org.junit.Assert.assertEquals
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

        val pages = TextPaginator.paginate(text, 120f, 80f, 18f, 1.5f, 1f)

        assertTrue(pages.size > 1)
        assertEquals(0, pages.first().startChar)
        assertEquals(text.length, pages.last().endChar)
        pages.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endChar, next.startChar)
        }
        assertEquals(text, pages.joinToString(separator = "") { it.text })
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
}
