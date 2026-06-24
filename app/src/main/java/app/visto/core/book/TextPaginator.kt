package app.visto.core.book

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

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
        val desiredLineHeight = (paint.textSize * spacingMultiplier).coerceAtLeast(1f)
        val fontMetrics = paint.fontMetrics
        val naturalLineHeight = (fontMetrics.descent - fontMetrics.ascent).coerceAtLeast(1f)
        val layout = buildLayout(
            text = text,
            paint = paint,
            widthPx = maxWidthPx.coerceAtLeast(1f).toInt().coerceAtLeast(1),
            lineSpacingExtraPx = (desiredLineHeight - naturalLineHeight).coerceAtLeast(0f),
        )

        val pages = mutableListOf<Page>()
        var pageStartLine = 0

        while (pageStartLine < layout.lineCount && layout.getLineStart(pageStartLine) < text.length) {
            val pageStart = layout.getLineStart(pageStartLine)
            val pageEndLine = measuredPageEndLine(
                layout = layout,
                startLine = pageStartLine,
                maxHeightPx = maxHeightPx.coerceAtLeast(1f),
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

    private fun measuredPageEndLine(
        layout: StaticLayout,
        startLine: Int,
        maxHeightPx: Float,
    ): Int {
        val startTop = layout.getLineTop(startLine)
        var endLine = startLine + 1
        while (endLine < layout.lineCount) {
            val nextEndLine = endLine + 1
            val measuredHeight = layout.getLineTop(nextEndLine) - startTop
            if (measuredHeight > maxHeightPx) return endLine
            endLine = nextEndLine
        }
        return endLine
    }
}
