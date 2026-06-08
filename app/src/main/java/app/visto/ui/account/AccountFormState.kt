package app.visto.ui.account

import app.visto.core.model.DavPath
import app.visto.data.webdav.WebDavDiagnosticResult

/**
 * Validation/UI state for the WebDAV account form.
 *
 * Kept as a pure data class so it can be exercised by JVM unit tests without
 * touching Compose or Android lifecycle.
 */
data class AccountFormState(
    val displayName: String = "",
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rootPath: String = "/",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val diagnostic: WebDavDiagnosticResult? = null,
) {
    val normalizedRootPath: String
        get() = DavPath.normalize(rootPath)

    val isValidUrl: Boolean
        get() {
            val trimmed = baseUrl.trim()
            return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                trimmed.length > 8
        }

    val isSafeRootPath: Boolean
        get() = !DavPath.hasDotSegments(rootPath)

    val canTestConnection: Boolean
        get() = !isTesting && !isSaving &&
            isValidUrl &&
            isSafeRootPath &&
            username.isNotBlank() &&
            password.isNotEmpty()

    val canSave: Boolean
        get() = canTestConnection && displayName.isNotBlank()
}

/**
 * Pure reducers used by the account ViewModel. Keeping these as functions
 * avoids exposing mutable Compose state to unit tests.
 */
object AccountFormReducer {

    fun updateBaseUrl(state: AccountFormState, value: String): AccountFormState =
        state.copy(baseUrl = value, errorMessage = null, diagnostic = null)

    fun updateUsername(state: AccountFormState, value: String): AccountFormState =
        state.copy(username = value, errorMessage = null, diagnostic = null)

    fun updatePassword(state: AccountFormState, value: String): AccountFormState =
        state.copy(password = value, errorMessage = null, diagnostic = null)

    fun updateDisplayName(state: AccountFormState, value: String): AccountFormState =
        state.copy(displayName = value)

    fun updateRootPath(state: AccountFormState, value: String): AccountFormState =
        state.copy(rootPath = value, diagnostic = null)

    fun setTesting(state: AccountFormState, testing: Boolean): AccountFormState =
        state.copy(
            isTesting = testing,
            errorMessage = if (testing) null else state.errorMessage,
            diagnostic = if (testing) null else state.diagnostic,
        )

    fun setSaving(state: AccountFormState, saving: Boolean): AccountFormState =
        state.copy(isSaving = saving)

    fun setError(state: AccountFormState, message: String): AccountFormState =
        state.copy(
            errorMessage = message,
            message = null,
            diagnostic = null,
            isTesting = false,
            isSaving = false,
        )

    fun setMessage(state: AccountFormState, message: String): AccountFormState =
        state.copy(message = message, errorMessage = null)

    fun setDiagnostic(state: AccountFormState, result: WebDavDiagnosticResult): AccountFormState =
        state.copy(
            diagnostic = result,
            message = null,
            errorMessage = null,
            isTesting = false,
            isSaving = false,
        )
}
