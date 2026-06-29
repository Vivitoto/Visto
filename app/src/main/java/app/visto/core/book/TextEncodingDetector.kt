package app.visto.core.book

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Lightweight text encoding detector for book files.
 *
 * The detector intentionally avoids external charset libraries. It covers the
 * common cases needed by the reader: Unicode BOMs, valid UTF-8, and CJK
 * byte-pattern heuristics.
 */
object TextEncodingDetector {
    private const val BYTE_PAIR_WEIGHT = 100
    private val cjkEncodings = listOf("Big5", "EUC-JP", "EUC-KR", "Shift_JIS", "GBK", "GB18030")
    private val simplifiedChineseHints = setOf(
        0x7B80, 0x4F53, 0x8BED, 0x56FD, 0x6D4B, 0x8BD5, 0x8BFB, 0x4E66, 0x95E8, 0x98CE,
        0x4E91, 0x9F99, 0x540E, 0x4F1A, 0x4E2A, 0x6765, 0x65F6, 0x957F, 0x65E0, 0x7231,
        0x4E50, 0x5E7F, 0x4E49, 0x4E0E, 0x4E07, 0x5185, 0x6C49, 0x8BF4,
    )
    private val traditionalChineseHints = setOf(
        0x7E41, 0x9AD4, 0x8A9E, 0x570B, 0x6E2C, 0x8A66, 0x95B1, 0x8B80, 0x66F8, 0x9580,
        0x98A8, 0x96F2, 0x9F8D, 0x5F8C, 0x6703, 0x500B, 0x4F86, 0x6642, 0x9577, 0x7121,
        0x611B, 0x6A02, 0x5EE3, 0x7FA9, 0x8207, 0x842C, 0x5167, 0x6F22, 0x8AAA, 0x81FA,
        0x7063,
    )
    private val commonChineseHints = setOf(
        0x4F60, 0x597D, 0x4E16, 0x754C, 0x7B2C, 0x7AE0, 0x4E2D, 0x6587, 0x5C0F, 0x7684,
        0x4E00, 0x662F, 0x4E0D, 0x4E86, 0x4EBA, 0x5728, 0x6709, 0x6211, 0x4ED6, 0x5979,
        0x5929, 0x5730, 0x5FC3, 0x751F, 0x5BB9,
    )
    private val japaneseKanjiHints = setOf(
        0x65E5, 0x672C, 0x8A9E, 0x6587, 0x7AE0, 0x6F22, 0x5B57, 0x7B2C, 0x4E00, 0x79C1,
        0x5F7C, 0x5973, 0x4ECA, 0x6642, 0x8AAD, 0x66F8, 0x6821,
    )

    fun detect(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "UTF-8"

        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return "UTF-8"
        }
        if (bytes.size >= 2) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                return "UTF-16LE"
            }
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                return "UTF-16BE"
            }
        }

        if (isValidUtf8(bytes)) return "UTF-8"

        return looksLikeCjk(bytes) ?: "UTF-8"
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xFF
            when {
                first <= 0x7F -> index += 1
                first in 0xC2..0xDF -> {
                    if (!hasContinuation(bytes, index + 1)) return false
                    index += 2
                }
                first in 0xE0..0xEF -> {
                    if (!hasContinuation(bytes, index + 1) || !hasContinuation(bytes, index + 2)) return false
                    val second = bytes[index + 1].toInt() and 0xFF
                    if (first == 0xE0 && second < 0xA0) return false
                    if (first == 0xED && second >= 0xA0) return false
                    index += 3
                }
                first in 0xF0..0xF4 -> {
                    if (!hasContinuation(bytes, index + 1) ||
                        !hasContinuation(bytes, index + 2) ||
                        !hasContinuation(bytes, index + 3)
                    ) {
                        return false
                    }
                    val second = bytes[index + 1].toInt() and 0xFF
                    if (first == 0xF0 && second < 0x90) return false
                    if (first == 0xF4 && second > 0x8F) return false
                    index += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun hasContinuation(bytes: ByteArray, index: Int): Boolean {
        if (index >= bytes.size) return false
        val value = bytes[index].toInt() and 0xFF
        return value in 0x80..0xBF
    }

    private fun looksLikeCjk(bytes: ByteArray): String? {
        return cjkEncodings
            .map { encoding ->
                val bytePairScore = countCjkBytePairs(bytes, encoding)
                val decodedTextScore = decodedTextScore(bytes, encoding)
                CjkScore(
                    encoding = encoding,
                    bytePairScore = bytePairScore,
                    totalScore = bytePairScore * BYTE_PAIR_WEIGHT + decodedTextScore + priorityFor(encoding),
                )
            }
            .filter { it.bytePairScore > 0 }
            .maxByOrNull { it.totalScore }
            ?.encoding
    }

    private fun countCjkBytePairs(bytes: ByteArray, encoding: String): Int =
        when (encoding) {
            "Big5" -> countPairs(bytes) { first, second ->
                first in 0xA1..0xF9 && (second in 0x40..0x7E || second in 0xA1..0xFE)
            }
            "EUC-JP" -> countEucJpSequences(bytes)
            "EUC-KR" -> countPairs(bytes) { first, second ->
                first in 0xA1..0xFE && second in 0xA1..0xFE
            }
            "Shift_JIS" -> countPairs(bytes) { first, second ->
                (first in 0x81..0x9F || first in 0xE0..0xFC) &&
                    (second in 0x40..0x7E || second in 0x80..0xFC)
            }
            "GBK" -> countPairs(bytes) { first, second ->
                first in 0x81..0xFE && (second in 0x40..0x7E || second in 0x80..0xFE)
            }
            "GB18030" -> countGb18030Sequences(bytes)
            else -> 0
        }

    private fun countPairs(bytes: ByteArray, matches: (Int, Int) -> Boolean): Int {
        var index = 0
        var pairs = 0
        while (index < bytes.size - 1) {
            val first = bytes[index].toInt() and 0xFF
            val second = bytes[index + 1].toInt() and 0xFF
            if (matches(first, second)) {
                pairs += 1
                index += 2
            } else {
                index += 1
            }
        }
        return pairs
    }

    private fun countEucJpSequences(bytes: ByteArray): Int {
        var index = 0
        var pairs = 0
        while (index < bytes.size - 1) {
            val first = bytes[index].toInt() and 0xFF
            val second = bytes[index + 1].toInt() and 0xFF
            val third = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            when {
                first == 0x8F && second in 0xA1..0xFE && third in 0xA1..0xFE -> {
                    pairs += 2
                    index += 3
                }
                first in 0xA1..0xFE && second in 0xA1..0xFE -> {
                    pairs += 1
                    index += 2
                }
                else -> index += 1
            }
        }
        return pairs
    }

    private fun countGb18030Sequences(bytes: ByteArray): Int {
        var index = 0
        var pairs = 0
        while (index < bytes.size - 1) {
            val first = bytes[index].toInt() and 0xFF
            val second = bytes[index + 1].toInt() and 0xFF
            val third = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            val fourth = if (index + 3 < bytes.size) bytes[index + 3].toInt() and 0xFF else -1
            when {
                first in 0x81..0xFE &&
                    second in 0x30..0x39 &&
                    third in 0x81..0xFE &&
                    fourth in 0x30..0x39 -> {
                    pairs += 2
                    index += 4
                }
                first in 0x81..0xFE &&
                    (second in 0x30..0x39 || second in 0x40..0x7E || second in 0x80..0xFE) -> {
                    pairs += 1
                    index += 2
                }
                else -> index += 1
            }
        }
        return pairs
    }

    private fun decodedTextScore(bytes: ByteArray, encoding: String): Int {
        val text = runCatching {
            Charset.forName(encoding).newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull() ?: return 0

        var han = 0
        var supplementaryHan = 0
        var kana = 0
        var hangul = 0
        var bopomofo = 0
        var cjkPunctuation = 0
        var suspicious = 0
        var simplified = 0
        var traditional = 0
        var commonChinese = 0
        var japaneseKanji = 0

        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            index += Character.charCount(codePoint)

            when {
                codePoint == 0xFFFD -> suspicious += 50
                Character.isISOControl(codePoint) &&
                    codePoint != '\n'.code &&
                    codePoint != '\r'.code &&
                    codePoint != '\t'.code -> suspicious += 25
                isHan(codePoint) -> {
                    han += 1
                    if (codePoint > 0xFFFF) supplementaryHan += 1
                    if (codePoint in simplifiedChineseHints) simplified += 1
                    if (codePoint in traditionalChineseHints) traditional += 1
                    if (codePoint in commonChineseHints) commonChinese += 1
                    if (codePoint in japaneseKanjiHints) japaneseKanji += 1
                }
                isKana(codePoint) -> kana += 1
                isHangul(codePoint) -> hangul += 1
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.BOPOMOFO -> bopomofo += 1
                isCjkPunctuationOrFullWidth(codePoint) -> cjkPunctuation += 1
            }
        }

        return when (encoding) {
            "Big5" -> han + cjkPunctuation +
                traditional * 14 +
                commonChinese * 8 -
                simplified * 2 -
                kana * 10 -
                hangul * 10 -
                bopomofo * 6 -
                suspicious
            "EUC-JP", "Shift_JIS" -> han + cjkPunctuation +
                kana * 10 +
                japaneseKanji * 12 -
                hangul * 10 -
                bopomofo * 6 -
                suspicious
            "EUC-KR" -> cjkPunctuation +
                hangul * 8 +
                han / 2 -
                kana * 10 -
                bopomofo * 6 -
                suspicious
            "GBK", "GB18030" -> han + cjkPunctuation +
                simplified * 14 +
                commonChinese * 14 +
                supplementaryHan * 8 -
                traditional * 3 -
                kana * 10 -
                hangul * 10 -
                bopomofo * 6 -
                suspicious
            else -> 0
        }
    }

    private fun isHan(codePoint: Int): Boolean =
        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN

    private fun isKana(codePoint: Int): Boolean {
        val script = Character.UnicodeScript.of(codePoint)
        return script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA
    }

    private fun isHangul(codePoint: Int): Boolean =
        Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL

    private fun isCjkPunctuationOrFullWidth(codePoint: Int): Boolean {
        val block = Character.UnicodeBlock.of(codePoint)
        return block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
            block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
    }

    private fun priorityFor(encoding: String): Int =
        when (encoding) {
            "GBK" -> 6
            "Big5" -> 5
            "EUC-JP" -> 4
            "EUC-KR" -> 3
            "Shift_JIS" -> 2
            "GB18030" -> 1
            else -> 0
        }

    private data class CjkScore(
        val encoding: String,
        val bytePairScore: Int,
        val totalScore: Int,
    )
}
