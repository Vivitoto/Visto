package app.visto.core.book

/**
 * Lightweight text encoding detector for book files.
 *
 * The detector intentionally avoids external charset libraries. It covers the
 * common cases needed by the reader: Unicode BOMs, valid UTF-8, and Chinese
 * GBK/GB18030 byte-pair patterns.
 */
object TextEncodingDetector {
    private const val SAMPLE_SIZE = 4 * 1024

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

        val sample = bytes.copyOfRange(0, minOf(bytes.size, SAMPLE_SIZE))
        if (isValidUtf8(sample)) return "UTF-8"

        return if (looksLikeGbk(sample)) "GBK" else "UTF-8"
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

    private fun looksLikeGbk(bytes: ByteArray): Boolean {
        var index = 0
        var gbkPairs = 0
        while (index < bytes.size - 1) {
            val first = bytes[index].toInt() and 0xFF
            val second = bytes[index + 1].toInt() and 0xFF
            if (first in 0x81..0xFE && second in 0x40..0xFE && second != 0x7F) {
                gbkPairs += 1
                index += 2
            } else {
                index += 1
            }
        }
        return gbkPairs > 0
    }
}
