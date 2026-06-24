package app.visto.core.book

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.floor

/** A single page slice inside a larger chapter text. */
data class Page(
    val startChar: Int,
    val endChar: Int,
    val text: String,
)

/** Splits plain text into pages using Android text measurement primitives. */
object TextPaginator {
    fun paginate(
        text: String,
        maxWidthPx: Float,
        maxHeightPx: Float,
        fontSizeSp: Float,
        lineSpacing: Float,
        density: Float,
        typeface: Typeface? = null,
    ): List<Page> {
        if (text.isEmpty()) return listOf(Page(startChar = 0, endChar = 0, text = ""))

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeSp * density
            this.typeface = typeface
        }
        val spacingMultiplier = lineSpacing.coerceAtLeast(0.1f)
        val lineHeight = (paint.textSize * spacingMultiplier).coerceAtLeast(1f)
        val linesPerPage = floor(maxHeightPx.coerceAtLeast(1f) / lineHeight)
            .toInt()
            .coerceAtLeast(1)
        val layout = buildLayout(
            text = text,
            paint = paint,
            widthPx = maxWidthPx.coerceAtLeast(1f).toInt().coerceAtLeast(1),
            lineSpacingExtraPx = (lineHeight - paint.textSize).coerceAtLeast(0f),
        )

        val pages = mutableListOf<Page>()
        var pageStartLine = 0

        while (pageStartLine < layout.lineCount && layout.getLineStart(pageStartLine) < text.length) {
            val pageStart = layout.getLineStart(pageStartLine)
            val naturalEndLine = (pageStartLine + linesPerPage).coerceAtMost(layout.lineCount)
            val pageEndLine = adjustedPageEndLine(
                text = text,
                layout = StaticLineLayout(layout),
                startLine = pageStartLine,
                naturalEndLine = naturalEndLine,
            )
            val pageEnd = layout.getLineEnd(pageEndLine - 1).coerceAtLeast(pageStart)
            pages += Page(
                startChar = pageStart,
                endChar = pageEnd,
                text = text.substring(pageStart, pageEnd),
            )
            pageStartLine = pageEndLine
        }

        return pages.ifEmpty { listOf(Page(startChar = 0, endChar = 0, text = "")) }
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        widthPx: Int,
        lineSpacingExtraPx: Float,
    ): StaticLayout =
        StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            paint,
            widthPx,
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setIncludePad(false)
            .setLineSpacing(lineSpacingExtraPx, 1f)
            .build()

    private fun adjustedPageEndLine(
        text: String,
        layout: PageLineLayout,
        startLine: Int,
        naturalEndLine: Int,
    ): Int {
        var endLine = naturalEndLine.coerceAtLeast(startLine + 1)
        val naturalPageLines = (naturalEndLine - startLine).coerceAtLeast(1)
        val minimumPageLines = if (naturalPageLines >= MIN_LINES_BEFORE_BACKTRACK) {
            MIN_LINES_AFTER_BACKTRACK
        } else {
            1
        }
        val minimumEndLine = (naturalEndLine - MAX_PAGE_BREAK_BACKTRACK_LINES)
            .coerceAtLeast(startLine + minimumPageLines)
        while (endLine > minimumEndLine) {
            val endChar = layout.getLineEnd(endLine - 1)
            val shouldMoveBreak =
                endsWithWeakPunctuationInsideParagraph(text, endChar) ||
                    leavesShortParagraphTail(text, layout, endLine)
            if (!shouldMoveBreak) return endLine
            endLine -= 1
        }
        return endLine
    }

    private fun endsWithWeakPunctuationInsideParagraph(text: String, endChar: Int): Boolean {
        val lastContentIndex = previousContentIndex(text, endChar)
        if (lastContentIndex < 0 || text[lastContentIndex] !in weakPageEndPunctuation) return false

        val paragraphEnd = paragraphEndAfter(text, lastContentIndex)
        val nextContentIndex = nextContentIndex(text, endChar, paragraphEnd)
        return nextContentIndex < paragraphEnd
    }

    private fun leavesShortParagraphTail(text: String, layout: PageLineLayout, nextLine: Int): Boolean {
        if (nextLine >= layout.lineCount) return false

        val breakOffset = layout.getLineStart(nextLine)
        val previousContentIndex = previousContentIndex(text, breakOffset)
        if (previousContentIndex < 0) return false

        val paragraphEnd = paragraphEndAfter(text, previousContentIndex)
        if (breakOffset >= paragraphEnd) return false

        val tailLineCount = lineCountUntil(layout, nextLine, paragraphEnd)
        val tailContentChars = text.substring(breakOffset, paragraphEnd).count { !it.isWhitespace() }
        return tailLineCount <= 1 && tailContentChars in 1..SHORT_PARAGRAPH_TAIL_CHARS
    }

    private fun lineCountUntil(layout: PageLineLayout, startLine: Int, endOffset: Int): Int {
        var line = startLine
        while (line < layout.lineCount && layout.getLineStart(line) < endOffset) {
            line += 1
        }
        return line - startLine
    }

    internal fun adjustedPageEndLineForTest(
        text: String,
        lineStarts: IntArray,
        lineEnds: IntArray,
        startLine: Int,
        naturalEndLine: Int,
    ): Int = adjustedPageEndLine(
        text = text,
        layout = ArrayLineLayout(lineStarts, lineEnds),
        startLine = startLine,
        naturalEndLine = naturalEndLine,
    )

    private interface PageLineLayout {
        val lineCount: Int
        fun getLineStart(line: Int): Int
        fun getLineEnd(line: Int): Int
    }

    private class StaticLineLayout(private val layout: StaticLayout) : PageLineLayout {
        override val lineCount: Int get() = layout.lineCount
        override fun getLineStart(line: Int): Int = layout.getLineStart(line)
        override fun getLineEnd(line: Int): Int = layout.getLineEnd(line)
    }

    private class ArrayLineLayout(
        private val lineStarts: IntArray,
        private val lineEnds: IntArray,
    ) : PageLineLayout {
        override val lineCount: Int get() = minOf(lineStarts.size, lineEnds.size)
        override fun getLineStart(line: Int): Int = lineStarts[line]
        override fun getLineEnd(line: Int): Int = lineEnds[line]
    }

    private fun previousContentIndex(text: String, offset: Int): Int {
        var index = offset.coerceAtMost(text.length) - 1
        while (index >= 0 && text[index].isWhitespace()) {
            index -= 1
        }
        return index
    }

    private fun nextContentIndex(text: String, start: Int, end: Int): Int {
        var index = start.coerceAtLeast(0)
        val safeEnd = end.coerceAtMost(text.length)
        while (index < safeEnd && text[index].isWhitespace()) {
            index += 1
        }
        return index
    }

    private fun paragraphEndAfter(text: String, offset: Int): Int =
        text.indexOf('\n', offset.coerceIn(0, text.length)).takeIf { it >= 0 } ?: text.length

    private const val SHORT_PARAGRAPH_TAIL_CHARS = 18
    private const val MIN_LINES_BEFORE_BACKTRACK = 2
    private const val MIN_LINES_AFTER_BACKTRACK = 2
    private const val MAX_PAGE_BREAK_BACKTRACK_LINES = 2

    private val weakPageEndPunctuation = setOf('，', '、', ',', '；', ';', '：', ':')
}
