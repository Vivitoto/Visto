package app.visto.ui.account

import app.visto.data.webdav.WebDavError
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountErrorMessagesTest {
    @Test
    fun authFailedMessage() {
        val msg = AccountErrorMessages.forWebDavError(WebDavError.AuthFailed())
        assertTrue(msg.contains("认证") || msg.contains("密码"))
    }

    @Test
    fun notFoundMessage() {
        val msg = AccountErrorMessages.forWebDavError(WebDavError.NotFound())
        assertTrue(msg.contains("找不到") || msg.contains("路径"))
    }

    @Test
    fun serverErrorMentionsStatus() {
        val msg = AccountErrorMessages.forWebDavError(WebDavError.ServerError(503))
        assertTrue(msg.contains("503"))
    }

    @Test
    fun networkErrorMessage() {
        val msg = AccountErrorMessages.forWebDavError(WebDavError.NetworkError("boom"))
        assertTrue(msg.contains("网络"))
    }

    @Test
    fun unexpectedExceptionFallsBack() {
        val msg = AccountErrorMessages.forWebDavError(IllegalStateException("nope"))
        assertTrue(msg.contains("nope") || msg.contains("未知"))
    }
}
