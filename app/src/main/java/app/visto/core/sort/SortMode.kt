package app.visto.core.sort

/**
 * Sort modes that the user can pick for the directory grid.
 *
 * Folders always come before files regardless of mode; the chosen mode
 * decides the ordering inside the folders block and inside the media block.
 */
enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    MODIFIED_NEWEST_FIRST,
    MODIFIED_OLDEST_FIRST,
    SIZE_LARGEST_FIRST,
    SIZE_SMALLEST_FIRST,
    TYPE,
    ;

    companion object {
        val DEFAULT: SortMode = MODIFIED_NEWEST_FIRST
    }
}
