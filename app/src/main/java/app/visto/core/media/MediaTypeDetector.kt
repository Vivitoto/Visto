package app.visto.core.media

/**
 * Detects [MediaType] for a remote WebDAV entry.
 *
 * Detection priority:
 *  1. MIME type from server when it is a usable media type.
 *  2. File extension fallback.
 *  3. UNKNOWN if nothing matched.
 *
 * v0.1 keeps detection pure and offline; no content sniffing.
 */
object MediaTypeDetector {
    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "heic", "heif", "bmp",
    )

    private val ANIMATED_IMAGE_EXTENSIONS = setOf(
        "gif", "webp",
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mov", "m4v", "webm", "mkv", "3gp",
    )

    /**
     * Detect a [MediaType] from a file [name] and optional [mimeType].
     *
     * The detector treats `.webp` as [MediaType.ANIMATED_IMAGE] because a
     * single static thumbnail of the first frame is safe for both static and
     * animated WebP, and the viewer can attempt animated playback.
     */
    fun detect(name: String, mimeType: String? = null): MediaType {
        val mimeResult = mimeType?.let { classifyMime(it) }
        if (mimeResult != null && mimeResult != MediaType.UNKNOWN) {
            return mimeResult
        }
        val extension = extensionOf(name)
        return when {
            extension == null -> if (mimeResult != null) MediaType.OTHER else MediaType.UNKNOWN
            extension in ANIMATED_IMAGE_EXTENSIONS -> MediaType.ANIMATED_IMAGE
            extension in IMAGE_EXTENSIONS -> MediaType.IMAGE
            extension in VIDEO_EXTENSIONS -> MediaType.VIDEO
            else -> MediaType.OTHER
        }
    }

    private fun classifyMime(mimeType: String): MediaType {
        val normalized = mimeType.trim().lowercase()
        if (normalized.isEmpty() || normalized == "application/octet-stream") {
            return MediaType.UNKNOWN
        }
        return when {
            normalized == "image/gif" -> MediaType.ANIMATED_IMAGE
            normalized == "image/webp" -> MediaType.ANIMATED_IMAGE
            normalized.startsWith("image/") -> MediaType.IMAGE
            normalized.startsWith("video/") -> MediaType.VIDEO
            else -> MediaType.OTHER
        }
    }

    private fun extensionOf(name: String): String? {
        val cleaned = name.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val dot = cleaned.lastIndexOf('.')
        if (dot <= 0 || dot == cleaned.length - 1) return null
        return cleaned.substring(dot + 1).lowercase()
    }
}
