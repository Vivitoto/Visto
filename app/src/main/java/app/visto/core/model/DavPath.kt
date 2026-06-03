package app.visto.core.model

/**
 * Pure WebDAV-style absolute path utilities.
 *
 * Conventions used across Visto:
 *  - Paths are absolute, starting with '/'.
 *  - The root path is "/" and has no parent.
 *  - Non-root paths never end with '/'.
 *  - Path segments are not URL-encoded at this layer; encoding is a network-layer concern.
 */
object DavPath {
    const val ROOT: String = "/"

    /**
     * Normalize an arbitrary [input] into a canonical absolute path.
     *
     * - null or blank input becomes "/".
     * - leading/trailing whitespace is trimmed.
     * - missing leading slash is added.
     * - repeated slashes are collapsed.
     * - trailing slash is removed unless the path is "/".
     */
    fun normalize(input: String?): String {
        val raw = input?.trim().orEmpty()
        if (raw.isEmpty()) return ROOT
        val withRoot = if (raw.startsWith('/')) raw else "/$raw"
        val collapsed = buildString(withRoot.length) {
            var lastSlash = false
            for (ch in withRoot) {
                if (ch == '/') {
                    if (!lastSlash) append(ch)
                    lastSlash = true
                } else {
                    append(ch)
                    lastSlash = false
                }
            }
        }
        if (collapsed == ROOT) return ROOT
        return collapsed.trimEnd('/').ifEmpty { ROOT }
    }

    /**
     * Join a normalized parent [parent] with a child [child] segment or sub-path.
     */
    fun join(parent: String, child: String): String {
        val base = normalize(parent)
        val tail = child.trim().trim('/')
        if (tail.isEmpty()) return base
        val raw = if (base == ROOT) "/$tail" else "$base/$tail"
        return normalize(raw)
    }

    /**
     * Return the parent path of [path], or null when [path] is the root.
     */
    fun parent(path: String): String? {
        val normalized = normalize(path)
        if (normalized == ROOT) return null
        val idx = normalized.lastIndexOf('/')
        if (idx <= 0) return ROOT
        return normalized.substring(0, idx)
    }

    /**
     * Return the last segment of [path], suitable for display.
     *
     * Returns "/" for the root path.
     */
    fun displayName(path: String): String {
        val normalized = normalize(path)
        if (normalized == ROOT) return ROOT
        return normalized.substringAfterLast('/')
    }

    /**
     * Return true when [child] is exactly one level below [parent].
     */
    fun isDirectChild(parent: String, child: String): Boolean {
        val normalizedParent = normalize(parent)
        val normalizedChild = normalize(child)
        val parentOfChild = parent(normalizedChild) ?: return false
        return parentOfChild == normalizedParent
    }
}
