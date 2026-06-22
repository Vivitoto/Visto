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
    private val PATTERNS = listOf(
        Regex("第[零一二三四五六七八九十百千万\\d]+[章节回卷][^\\n]*"),
        Regex("Chapter\\s+\\d+[^\\n]*", RegexOption.IGNORE_CASE),
    )
    private val LINE_PATTERNS = listOf(
        Regex("^第[零一二三四五六七八九十百千万\\d]+[章节回卷][^\\n]*$"),
        Regex("^Chapter\\s+\\d+[^\\n]*$", RegexOption.IGNORE_CASE),
    )

    fun parse(text: String): List<Chapter> {
        val matches = PATTERNS
            .flatMapIndexed { priority, regex ->
                regex.findAll(text).map { match ->
                    ChapterMatch(
                        title = match.value.trim(),
                        startOffset = match.range.first,
                        matchEndOffset = match.range.last + 1,
                        priority = priority,
                    )
                }
            }
            .sortedWith(compareBy<ChapterMatch> { it.startOffset }.thenBy { it.priority })
            .filterOverlapping()

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

    private fun List<ChapterMatch>.filterOverlapping(): List<ChapterMatch> {
        val filtered = mutableListOf<ChapterMatch>()
        var previousEnd = -1
        for (match in this) {
            if (match.startOffset >= previousEnd) {
                filtered += match
                previousEnd = match.matchEndOffset
            }
        }
        return filtered
    }

    internal fun isChapterHeadingLine(line: String): Boolean =
        LINE_PATTERNS.any { it.matches(line.trimReaderHeadingWhitespace()) }

    private data class ChapterMatch(
        val title: String,
        val startOffset: Int,
        val matchEndOffset: Int,
        val priority: Int,
    )

    private fun String.trimReaderHeadingWhitespace(): String =
        trim { it == ' ' || it == '\t' || it == '\u3000' }
}
