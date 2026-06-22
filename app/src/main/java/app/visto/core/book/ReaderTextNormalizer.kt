package app.visto.core.book

/** Normalizes plain-text book content before chapter parsing and pagination. */
object ReaderTextNormalizer {
    private const val FULL_WIDTH_FIRST_LINE_INDENT = "\u3000\u3000"
    private val markdownPreservePatterns = listOf(
        Regex("^#{1,6}(\\s|$).*"),
        Regex("^[-*+]\\s+.*"),
        Regex("^\\d+[.)、]\\s*\\S.*"),
        Regex("^>\\s?.*"),
    )

    fun normalize(text: String): String {
        if (text.isEmpty()) return text

        val output = mutableListOf<String>()
        var inFencedBlock = false

        normalizeLineEndings(text).split('\n').forEach { rawLine ->
            val line = rawLine.trimTrailingReaderWhitespace()
            if (line.isEmpty()) return@forEach

            val fenceLine = line.isFenceLine()
            output += if (inFencedBlock || fenceLine) {
                line
            } else {
                line.withReaderIndent()
            }
            if (fenceLine) {
                inFencedBlock = !inFencedBlock
            }
        }

        return output.joinToString(separator = "\n")
    }

    private fun normalizeLineEndings(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    private fun String.withReaderIndent(): String {
        if (hasLeadingReaderIndent() || ChapterParser.isChapterHeadingLine(this) || shouldPreserveMarkdownLine()) {
            return this
        }
        return FULL_WIDTH_FIRST_LINE_INDENT + this
    }

    private fun String.trimTrailingReaderWhitespace(): String =
        trimEnd { it == ' ' || it == '\t' || it == '\u3000' }

    private fun String.hasLeadingReaderIndent(): Boolean =
        firstOrNull()?.let { it == ' ' || it == '\t' || it == '\u3000' } == true

    private fun String.shouldPreserveMarkdownLine(): Boolean =
        markdownPreservePatterns.any { it.matches(this) }

    private fun String.isFenceLine(): Boolean {
        val trimmedLine = trimStart { it == ' ' || it == '\t' || it == '\u3000' }
        return trimmedLine.startsWith("```") || trimmedLine.startsWith("~~~")
    }
}
