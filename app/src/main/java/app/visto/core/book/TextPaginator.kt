package app.visto.core.book

import android.graphics.Paint
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
    ): List<Page> {
        if (text.isEmpty()) return listOf(Page(startChar = 0, endChar = 0, text = ""))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeSp * density
        }
        val spacingMultiplier = lineSpacing.coerceAtLeast(0.1f)
        val lineHeight = (paint.textSize * spacingMultiplier).coerceAtLeast(1f)
        val linesPerPage = floor(maxHeightPx.coerceAtLeast(1f) / lineHeight)
            .toInt()
            .coerceAtLeast(1)
        val maxLineWidth = maxWidthPx.coerceAtLeast(1f)

        val pages = mutableListOf<Page>()
        var pageStart = 0
        var lineCount = 0
        var cursor = 0

        while (cursor < text.length) {
            val lineEnd = nextLineEnd(text, cursor, maxLineWidth, paint)
            lineCount += 1

            if (lineCount == linesPerPage) {
                pages += Page(
                    startChar = pageStart,
                    endChar = lineEnd,
                    text = text.substring(pageStart, lineEnd),
                )
                pageStart = lineEnd
                lineCount = 0
            }

            cursor = lineEnd
        }

        if (pageStart < text.length) {
            pages += Page(
                startChar = pageStart,
                endChar = text.length,
                text = text.substring(pageStart),
            )
        }

        return pages.ifEmpty { listOf(Page(startChar = 0, endChar = 0, text = "")) }
    }

    private fun nextLineEnd(
        text: String,
        start: Int,
        maxLineWidth: Float,
        paint: Paint,
    ): Int {
        if (text[start] == '\n') return start + 1

        val paragraphEnd = text.indexOf('\n', start).takeIf { it >= 0 } ?: text.length
        val measuredWidth = FloatArray(1)
        val breakCount = paint.breakText(
            text,
            start,
            paragraphEnd,
            true,
            maxLineWidth,
            measuredWidth,
        ).coerceAtLeast(1)
        var lineEnd = (start + breakCount).coerceAtMost(paragraphEnd)

        if (lineEnd == paragraphEnd && lineEnd < text.length && text[lineEnd] == '\n') {
            lineEnd += 1
        }
        return lineEnd
    }
}
