package app.visto.ui.account

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
) {
    val normalizedRootPath: String
        get() = rootPath.ifBlank { "/" }

    val isValidUrl: Boolean
        get() {
            val trimmed = baseUrl.trim()
            return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                trimmed.length > 8
        }

    val canTestConnection: Boolean
        get() = !isTesting && !isSaving &&
            isValidUrl &&
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
        state.copy(baseUrl = value, errorMessage = null)

    fun updateUsername(state: AccountFormState, value: String): AccountFormState =
        state.copy(username = value, errorMessage = null)

    fun updatePassword(state: AccountFormState, value: String): AccountFormState =
        state.copy(password = value, errorMessage = null)

    fun updateDisplayName(state: AccountFormState, value: String): AccountFormState =
        state.copy(displayName = value)

    fun updateRootPath(state: AccountFormState, value: String): AccountFormState =
        state.copy(rootPath = value)

    fun setTesting(state: AccountFormState, testing: Boolean): AccountFormState =
        state.copy(isTesting = testing, errorMessage = if (testing) null else state.errorMessage)

    fun setSaving(state: AccountFormState, saving: Boolean): AccountFormState =
        state.copy(isSaving = saving)

    fun setError(state: AccountFormState, message: String): AccountFormState =
        state.copy(errorMessage = message, isTesting = false, isSaving = false)

    fun setMessage(state: AccountFormState, message: String): AccountFormState =
        state.copy(message = message, errorMessage = null)
}
