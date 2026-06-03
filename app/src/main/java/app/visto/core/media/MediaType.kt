package app.visto.core.media

/**
 * Classification of a remote WebDAV entry, used to decide thumbnail strategy
 * and viewer rendering.
 *
 * Visto v0.1 is read-only and does not edit or upload media; this type is for
 * display routing only.
 */
enum class MediaType {
    /** Plain non-animated image: JPEG, PNG, HEIC, static WebP. */
    IMAGE,

    /** Animated raster image: GIF or animated WebP. Grid uses first frame. */
    ANIMATED_IMAGE,

    /** Video container playable by Media3/ExoPlayer in detail view. */
    VIDEO,

    /** Known file type that is not image or video, hidden from media grid by default. */
    OTHER,

    /** Could not determine a usable type. */
    UNKNOWN,
}
