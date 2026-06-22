package app.visto.core.book

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTextNormalizerTest {

    @Test
    fun blankLinesAreRemoved() {
        val text = "第一段\r\n\r\n   \r\n第二段\r第三段   "

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("\u3000\u3000第一段\n\u3000\u3000第二段\n\u3000\u3000第三段", normalized)
    }

    @Test
    fun normalParagraphLinesReceiveFullWidthFirstLineIndent() {
        val normalized = ReaderTextNormalizer.normalize("这是正文")

        assertEquals("\u3000\u3000这是正文", normalized)
    }

    @Test
    fun chapterHeadingLinesAreNotIndented() {
        val text = "第一章 开端\n正文\nChapter 2 Next"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("第一章 开端\n\u3000\u3000正文\nChapter 2 Next", normalized)
    }

    @Test
    fun alreadyIndentedLinesAreNotDoubleIndented() {
        val text = "  空格缩进\n\t制表符缩进\n\u3000\u3000全角缩进"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("  空格缩进\n\t制表符缩进\n\u3000\u3000全角缩进", normalized)
    }

    @Test
    fun markdownHeadingsAndListsAreNotIndented() {
        val text = "# 标题\n- 项目\n1. 编号\n> 引用\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("# 标题\n- 项目\n1. 编号\n> 引用\n\u3000\u3000正文", normalized)
    }

    @Test
    fun fencedCodeBlocksAreNotIndented() {
        val text = "```kotlin\nprintln(\"hi\")\n```\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("```kotlin\nprintln(\"hi\")\n```\n\u3000\u3000正文", normalized)
    }
}
