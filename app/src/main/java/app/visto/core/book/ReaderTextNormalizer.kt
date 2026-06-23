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
    private val cjkReaderScripts = setOf(
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.HANGUL,
        Character.UnicodeScript.BOPOMOFO,
    )
    private val cjkReaderBlocks = setOf(
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS,
    )

    fun normalize(text: String): String {
        if (text.isEmpty()) return text

        val output = mutableListOf<String>()
        val paragraphLines = mutableListOf<String>()
        var inFencedBlock = false

        fun flushParagraph() {
            if (paragraphLines.isEmpty()) return
            output += FULL_WIDTH_FIRST_LINE_INDENT + paragraphLines.joinProseLines()
            paragraphLines.clear()
        }

        fun separateParagraph() {
            flushParagraph()
        }

        normalizeLineEndings(text).split('\n').forEach { rawLine ->
            val line = rawLine.trimTrailingReaderWhitespace()

            if (inFencedBlock) {
                output += line
                if (line.isFenceLine()) {
                    inFencedBlock = false
                }
                return@forEach
            }

            if (line.isEmpty()) {
                separateParagraph()
                return@forEach
            }

            if (line.isFenceLine()) {
                flushParagraph()
                output += line
                inFencedBlock = true
                return@forEach
            }

            if (line.isStandaloneReaderLine()) {
                flushParagraph()
                output += line
                return@forEach
            }

            if (paragraphLines.isNotEmpty() && line.hasLeadingReaderIndent()) {
                flushParagraph()
            }
            val paragraphLine = line.trimReaderParagraphWhitespace()
            if (paragraphLine.isNotEmpty()) {
                paragraphLines += paragraphLine
            }
        }

        flushParagraph()
        while (output.lastOrNull()?.isEmpty() == true) {
            output.removeAt(output.lastIndex)
        }

        return output.joinToString(separator = "\n")
    }

    private fun normalizeLineEndings(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    private fun String.trimTrailingReaderWhitespace(): String =
        trimEnd { it == ' ' || it == '\t' || it == '\u3000' }

    private fun String.trimReaderParagraphWhitespace(): String =
        trim { it == ' ' || it == '\t' || it == '\u3000' }

    private fun String.hasLeadingReaderIndent(): Boolean =
        firstOrNull()?.let { it == ' ' || it == '\t' || it == '\u3000' } == true

    private fun String.isStandaloneReaderLine(): Boolean =
        ChapterParser.isChapterHeadingLine(this) || shouldPreserveMarkdownLine()

    private fun String.shouldPreserveMarkdownLine(): Boolean =
        markdownPreservePatterns.any { it.matches(trimReaderParagraphLeadingWhitespace()) }

    private fun String.isFenceLine(): Boolean {
        val trimmedLine = trimStart { it == ' ' || it == '\t' || it == '\u3000' }
        return trimmedLine.startsWith("```") || trimmedLine.startsWith("~~~")
    }

    private fun String.trimReaderParagraphLeadingWhitespace(): String =
        trimStart { it == ' ' || it == '\t' || it == '\u3000' }

    private fun List<String>.joinProseLines(): String {
        val merged = StringBuilder()
        for (line in this) {
            if (line.isEmpty()) continue
            if (merged.isNotEmpty() && shouldInsertSpaceBetween(merged[merged.length - 1], line.first())) {
                merged.append(' ')
            }
            merged.append(line)
        }
        return merged.toString()
    }

    private fun shouldInsertSpaceBetween(previous: Char, next: Char): Boolean {
        if (previous.isCjkReaderCharacter() || next.isCjkReaderCharacter()) return false
        if (previous == '-' || previous.isAsciiOpeningPunctuation() || next.isAsciiClosingPunctuation()) return false
        return previous.isAsciiReaderBoundary() && next.isAsciiReaderBoundary()
    }

    private fun Char.isAsciiReaderBoundary(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in ".,;:!?)\"'"

    private fun Char.isAsciiOpeningPunctuation(): Boolean =
        this in "([{\"'"

    private fun Char.isAsciiClosingPunctuation(): Boolean =
        this in ".,;:!?)%]}\"'"

    private fun Char.isCjkReaderCharacter(): Boolean =
        Character.UnicodeScript.of(code) in cjkReaderScripts || Character.UnicodeBlock.of(this) in cjkReaderBlocks
}
