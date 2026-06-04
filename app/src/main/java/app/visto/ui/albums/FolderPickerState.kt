package app.visto.ui.albums

import app.visto.core.model.RemoteEntry

/**
 * State for selecting a WebDAV folder path while adding an album.
 */
data class FolderPickerState(
    val currentPath: String,
    val folders: List<RemoteEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val canGoUp: Boolean get() = currentPath != "/"
}

object FolderPickerNavigator {
    fun parentOf(path: String): String {
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
}
