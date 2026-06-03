package app.visto.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountFormStateTest {

    @Test
    fun emptyFormCannotTestOrSave() {
        val state = AccountFormState()
        assertFalse(state.canTestConnection)
        assertFalse(state.canSave)
        assertEquals("/", state.normalizedRootPath)
    }

    @Test
    fun invalidUrlBlocksTesting() {
        val state = AccountFormState(
            baseUrl = "not-a-url",
            username = "alice",
            password = "secret",
        )
        assertFalse(state.canTestConnection)
    }

    @Test
    fun validFormCanTestConnection() {
        val state = AccountFormState(
            baseUrl = "https://dav.example.com/dav",
            username = "alice",
            password = "secret",
        )
        assertTrue(state.canTestConnection)
        assertFalse("save still needs displayName", state.canSave)
    }

    @Test
    fun displayNameUnlocksSave() {
        val state = AccountFormState(
            displayName = "Home",
            baseUrl = "https://dav.example.com/dav",
            username = "alice",
            password = "secret",
        )
        assertTrue(state.canSave)
    }

    @Test
    fun reducerClearsErrorOnNewInput() {
        val state = AccountFormState(errorMessage = "old")
        val updated = AccountFormReducer.updateBaseUrl(state, "https://dav.example.com")
        assertNull(updated.errorMessage)
        assertEquals("https://dav.example.com", updated.baseUrl)
    }

    @Test
    fun reducerSettingErrorStopsTestingAndSaving() {
        val state = AccountFormState(isTesting = true, isSaving = true)
        val updated = AccountFormReducer.setError(state, "401")
        assertEquals("401", updated.errorMessage)
        assertFalse(updated.isTesting)
        assertFalse(updated.isSaving)
    }
}
