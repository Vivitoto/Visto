package app.visto.ui.albums

import app.visto.core.model.DavPath
import app.visto.data.db.AlbumSourceEntity

/**
 * UI state for the album list (home) screen.
 */
data class AlbumListUiState(
    val albums: List<AlbumSourceEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val addDialog: AlbumAddFormState = AlbumAddFormState(),
)

data class AlbumAddFormState(
    val name: String = "",
    val path: String = "",
    val nameAutoFilled: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

object AlbumAddFormReducer {

    fun updatePath(state: AlbumAddFormState, newPath: String): AlbumAddFormState {
        val derivedName = derivedNameFor(newPath)
        val nextName = if (state.nameAutoFilled || state.name.isBlank()) derivedName else state.name
        return state.copy(path = newPath, name = nextName, errorMessage = null)
    }

    fun updateName(state: AlbumAddFormState, newName: String): AlbumAddFormState {
        return state.copy(name = newName, nameAutoFilled = false, errorMessage = null)
    }

    fun setError(state: AlbumAddFormState, message: String): AlbumAddFormState {
        return state.copy(errorMessage = message, isSaving = false)
    }

    fun setSaving(state: AlbumAddFormState, saving: Boolean): AlbumAddFormState {
        return state.copy(isSaving = saving, errorMessage = if (saving) null else state.errorMessage)
    }

    fun reset(): AlbumAddFormState = AlbumAddFormState()

    private fun derivedNameFor(path: String): String {
        val trimmed = path.trim().trimEnd('/')
        if (trimmed.isEmpty() || trimmed == "/") return ""
        return trimmed.substringAfterLast('/')
    }
}

/**
 * Pure validation for the add-album form. Returns either a normalized
 * (name, path) pair or an error message.
 */
object AlbumAddValidator {

    sealed interface Result {
        data class Ok(val name: String, val path: String) : Result
        data class Err(val message: String) : Result
    }

    fun validate(state: AlbumAddFormState, existingPaths: Set<String>): Result {
        val trimmedPath = state.path.trim()
        if (trimmedPath.isEmpty()) {
            return Result.Err(app.visto.ui.Strings.ALBUMS_ERR_PATH_REQUIRED)
        }
        if (!trimmedPath.startsWith("/")) {
            return Result.Err(app.visto.ui.Strings.ALBUMS_ERR_PATH_MUST_START_WITH_SLASH)
        }
        val normalizedPath = DavPath.normalize(trimmedPath)
        if (DavPath.hasDotSegments(normalizedPath)) {
            return Result.Err(app.visto.ui.Strings.ERR_INVALID_PATH)
        }
        val normalizedExistingPaths = existingPaths.mapTo(mutableSetOf()) { DavPath.normalize(it) }
        if (normalizedPath in normalizedExistingPaths) {
            return Result.Err(app.visto.ui.Strings.ALBUMS_ERR_DUPLICATE)
        }
        val finalName = state.name.trim().ifEmpty { normalizedPath.substringAfterLast('/').ifEmpty { normalizedPath } }
        return Result.Ok(name = finalName, path = normalizedPath)
    }
}
