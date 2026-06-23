package app.visto.core.book

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTextNormalizerTest {

    @Test
    fun hardWrappedChineseProseLinesMergeIntoSingleIndentedParagraph() {
        val text = "这是很长的一段文字，\n它在原始文件里被硬换行，\n但阅读器应该按宽度重新排版。"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("\u3000\u3000这是很长的一段文字，它在原始文件里被硬换行，但阅读器应该按宽度重新排版。", normalized)
    }

    @Test
    fun normalParagraphLinesReceiveFullWidthFirstLineIndent() {
        val normalized = ReaderTextNormalizer.normalize("这是正文")

        assertEquals("\u3000\u3000这是正文", normalized)
    }

    @Test
    fun blankLinesSeparateParagraphsWithoutVisibleBlankDisplayLines() {
        val text = "第一段的第一行，\n第二行仍是一段。\n\n   \n第二段的第一行，\r第二行仍是一段。"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals(
            "\u3000\u3000第一段的第一行，第二行仍是一段。\n\u3000\u3000第二段的第一行，第二行仍是一段。",
            normalized,
        )
    }

    @Test
    fun chapterHeadingLinesAreNotIndented() {
        val text = "第一章 开端\n正文第一行，\n正文第二行。\nChapter 2 Next\n第二段正文第一行，\n第二段正文第二行。"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals(
            "第一章 开端\n\u3000\u3000正文第一行，正文第二行。\nChapter 2 Next\n\u3000\u3000第二段正文第一行，第二段正文第二行。",
            normalized,
        )
    }

    @Test
    fun existingReaderIndentsStartNewSingleIndentedParagraphs() {
        val text = "\u3000\u3000第一段已经缩进。\n\u3000\u3000第二段也已经缩进。"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("\u3000\u3000第一段已经缩进。\n\u3000\u3000第二段也已经缩进。", normalized)
    }

    @Test
    fun markdownHeadingsAndListsAreNotIndented() {
        val text = "# 标题\n- 项目\n1. 编号\n> 引用\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("# 标题\n- 项目\n1. 编号\n> 引用\n\u3000\u3000正文", normalized)
    }

    @Test
    fun blankLinesAroundMarkdownStandaloneLinesDoNotCreateDisplayGaps() {
        val text = "# 标题\n\n- 项目\n\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("# 标题\n- 项目\n\u3000\u3000正文", normalized)
    }

    @Test
    fun fencedCodeBlocksAreNotIndented() {
        val text = "```kotlin\nprintln(\"hi\")\n```\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("```kotlin\nprintln(\"hi\")\n```\n\u3000\u3000正文", normalized)
    }

    @Test
    fun fencedCodeBlockInternalBlankLinesArePreserved() {
        val text = "```kotlin\nval a = 1\n\nval b = 2\n```\n\n正文"

        val normalized = ReaderTextNormalizer.normalize(text)

        assertEquals("```kotlin\nval a = 1\n\nval b = 2\n```\n\u3000\u3000正文", normalized)
    }
}
