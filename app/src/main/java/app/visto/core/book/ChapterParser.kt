package app.visto.core.book

/** A detected chapter boundary in a plain-text book. */
data class Chapter(
    val index: Int,
    val title: String,
    val startOffset: Int,
    val endOffset: Int,
)

/** Parses simple Chinese and English chapter headings from full book text. */
object ChapterParser {
    private val LINE_PATTERNS = listOf(
        Regex("^第[零一二三四五六七八九十百千万\\d]+[章节回卷][^\\n]*$"),
        Regex("^Chapter\\s+\\d+[^\\n]*$", RegexOption.IGNORE_CASE),
    )

    fun parse(text: String): List<Chapter> {
        val matches = findChapterMatches(text)

        if (matches.isEmpty()) {
            return listOf(
                Chapter(
                    index = 0,
                    title = "全文",
                    startOffset = 0,
                    endOffset = text.length,
                ),
            )
        }

        return matches.mapIndexed { index, match ->
            Chapter(
                index = index,
                title = match.title,
                startOffset = match.startOffset,
                endOffset = matches.getOrNull(index + 1)?.startOffset ?: text.length,
            )
        }
    }

    private fun findChapterMatches(text: String): List<ChapterMatch> {
        val matches = mutableListOf<ChapterMatch>()
        var lineStart = 0

        while (lineStart <= text.length) {
            val newlineOffset = text.indexOf('\n', lineStart)
            val lineEnd = if (newlineOffset == -1) text.length else newlineOffset
            val contentEnd = if (lineEnd > lineStart && text[lineEnd - 1] == '\r') {
                lineEnd - 1
            } else {
                lineEnd
            }
            val line = text.substring(lineStart, contentEnd)
            val title = line.trimReaderHeadingWhitespace()

            if (title.isChapterHeadingTitle()) {
                matches += ChapterMatch(
                    title = title,
                    startOffset = lineStart,
                )
            }

            if (newlineOffset == -1) break
            lineStart = newlineOffset + 1
        }

        return matches
    }

    internal fun isChapterHeadingLine(line: String): Boolean =
        line.trimReaderHeadingWhitespace().isChapterHeadingTitle()

    private data class ChapterMatch(
        val title: String,
        val startOffset: Int,
    )

    private fun String.isChapterHeadingTitle(): Boolean =
        LINE_PATTERNS.any { it.matches(this) }

    private fun String.trimReaderHeadingWhitespace(): String =
        trim { it == ' ' || it == '\t' || it == '\u3000' }
}
