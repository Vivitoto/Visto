package app.visto.core.sort

/**
 * Sort modes that the user can pick for the directory grid.
 *
 * Folders always come before files regardless of mode; the chosen mode
 * decides the ordering inside the folders block and inside the media block.
 */
enum class SortMode(val storageKey: String) {
    NAME_ASC("name_asc"),
    NAME_DESC("name_desc"),
    MODIFIED_NEWEST_FIRST("modified_newest_first"),
    MODIFIED_OLDEST_FIRST("modified_oldest_first"),
    SIZE_LARGEST_FIRST("size_largest_first"),
    SIZE_SMALLEST_FIRST("size_smallest_first"),
    TYPE("type"),
    ;

    companion object {
        val DEFAULT: SortMode = MODIFIED_NEWEST_FIRST

        fun fromStorage(value: String?): SortMode {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.storageKey == value || it.name == value } ?: DEFAULT
        }
    }
}
