package app.visto.ui.albums

import app.visto.core.model.RemoteEntry

/**
 * State for selecting a WebDAV folder path while adding an album.
 */
data class FolderPickerState(
    val currentPath: String,
    val rootPath: String = "/",
    val folders: List<RemoteEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val canGoUp: Boolean get() = currentPath != rootPath
}

object FolderPickerNavigator {
    fun parentOf(path: String, rootPath: String = "/"): String {
        val root = normalize(rootPath)
        val current = clampToRoot(path, root)
        if (current == root) return root
        val parent = rawParentOf(current)
        return clampToRoot(parent, root)
    }

    private fun rawParentOf(path: String): String {
        val normalized = normalize(path)
        if (normalized == "/") return "/"
        val trimmed = normalized.trimEnd('/')
        val idx = trimmed.lastIndexOf('/')
        return if (idx <= 0) "/" else trimmed.substring(0, idx)
    }

    fun normalize(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "/") return "/"
        return "/" + trimmed.trim('/').replace(Regex("/+"), "/")
    }

    fun clampToRoot(path: String, rootPath: String): String {
        val root = normalize(rootPath)
        val normalized = normalize(path)
        if (root == "/") return normalized
        return if (normalized == root || normalized.startsWith("$root/")) normalized else root
    }
}
