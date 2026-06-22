package app.visto.core.book

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterParserTest {

    @Test
    fun singleChineseNumberedChapterIsDetected() {
        val text = "第1章 开端\n正文..."

        val chapters = ChapterParser.parse(text)

        assertEquals(1, chapters.size)
        assertEquals(0, chapters[0].index)
        assertEquals("第1章 开端", chapters[0].title)
        assertEquals(0, chapters[0].startOffset)
        assertEquals(text.length, chapters[0].endOffset)
    }

    @Test
    fun multipleChineseChaptersAreDetected() {
        val text = "第一章 开端\n正文\n第二章 进入\n更多正文"

        val chapters = ChapterParser.parse(text)

        assertEquals(2, chapters.size)
        assertEquals("第一章 开端", chapters[0].title)
        assertEquals("第二章 进入", chapters[1].title)
        assertEquals(text.indexOf("第二章"), chapters[0].endOffset)
        assertEquals(text.length, chapters[1].endOffset)
    }

    @Test
    fun commonChineseChapterMarkersAreDetected() {
        val text = "第零章 序\n第二百三十四回 归来\n第3节 收尾"

        val chapters = ChapterParser.parse(text)

        assertEquals(3, chapters.size)
        assertEquals("第零章 序", chapters[0].title)
        assertEquals("第二百三十四回 归来", chapters[1].title)
        assertEquals("第3节 收尾", chapters[2].title)
    }

    @Test
    fun textWithoutChapterMarkersReturnsFullTextChapter() {
        val text = "只是一些没有章节标记的正文。"

        val chapters = ChapterParser.parse(text)

        assertEquals(1, chapters.size)
        assertEquals("全文", chapters[0].title)
        assertEquals(0, chapters[0].startOffset)
        assertEquals(text.length, chapters[0].endOffset)
    }

    @Test
    fun englishChaptersAreDetectedCaseInsensitively() {
        val text = "Chapter 1 Start\nbody\nchapter 2 Next\n"

        val chapters = ChapterParser.parse(text)

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1 Start", chapters[0].title)
        assertEquals("chapter 2 Next", chapters[1].title)
    }

    @Test
    fun mixedChineseAndEnglishChaptersAreAllDetected() {
        val text = "第1章 开端\n正文\nChapter 2 Next\n正文"

        val chapters = ChapterParser.parse(text)

        assertEquals(2, chapters.size)
        assertEquals("第1章 开端", chapters[0].title)
        assertEquals("Chapter 2 Next", chapters[1].title)
        assertEquals(text.indexOf("Chapter 2"), chapters[0].endOffset)
        assertEquals(text.length, chapters[1].endOffset)
    }
}
