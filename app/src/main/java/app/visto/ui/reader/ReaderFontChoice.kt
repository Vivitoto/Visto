package app.visto.ui.reader

/**
 * User-selected font family for plain-text reader content.
 *
 * Custom fonts are stored as file names under the app-private reader font
 * directory, not as persisted document Uris.
 */
sealed class ReaderFontChoice(val storageKey: String) {
    data object SystemDefault : ReaderFontChoice(SYSTEM_STORAGE_KEY)
    data object Sans : ReaderFontChoice(SANS_STORAGE_KEY)
    data object Serif : ReaderFontChoice(SERIF_STORAGE_KEY)
    data class Custom(val fileName: String) : ReaderFontChoice("$CUSTOM_PREFIX$fileName")

    companion object {
        const val SYSTEM_STORAGE_KEY = "system"
        const val SANS_STORAGE_KEY = "sans"
        const val SERIF_STORAGE_KEY = "serif"
        const val CUSTOM_PREFIX = "custom:"

        val BUILT_IN = listOf(SystemDefault, Sans, Serif)
        val DEFAULT = SystemDefault

        fun fromStorage(value: String?): ReaderFontChoice {
            val trimmed = value?.trim().orEmpty()
            return when (trimmed.lowercase()) {
                SYSTEM_STORAGE_KEY, "" -> SystemDefault
                SANS_STORAGE_KEY -> Sans
                SERIF_STORAGE_KEY -> Serif
                else -> customFromStorage(trimmed) ?: SystemDefault
            }
        }

        fun sanitizeStorageKey(value: String?): String = fromStorage(value).storageKey

        private fun customFromStorage(value: String): ReaderFontChoice? {
            if (!value.startsWith(CUSTOM_PREFIX, ignoreCase = true)) return null
            val fileName = value.substringAfter(':')
            return if (isSafeCustomFileName(fileName)) {
                Custom(fileName)
            } else {
                null
            }
        }

        fun isSafeCustomFileName(fileName: String): Boolean {
            if (fileName.isBlank()) return false
            if (fileName.any { it == '/' || it == '\\' || it == ':' }) return false
            val lower = fileName.lowercase()
            return lower.endsWith(".ttf") || lower.endsWith(".otf")
        }
    }
}
