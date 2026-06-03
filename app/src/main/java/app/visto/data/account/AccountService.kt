package app.visto.data.account

import app.visto.data.db.DavAccountDao
import app.visto.data.db.DavAccountEntity
import app.visto.data.webdav.WebDavCredentials

/**
 * Coordinates [DavAccountEntity] persistence with [CredentialStore].
 *
 * Visto v0.1 manages a single active account, but the schema and this
 * service keep account ids first-class so additional accounts can be added
 * later without rewiring callers.
 */
class AccountService(
    private val accountDao: DavAccountDao,
    private val credentialStore: CredentialStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun activeAccount(): AccountSummary? {
        val entity = accountDao.getActive() ?: return null
        return AccountSummary(
            id = entity.id,
            displayName = entity.displayName,
            baseUrl = entity.baseUrl,
            rootPath = entity.rootPath,
            username = entity.username,
            credentialRef = entity.credentialRef,
        )
    }

    suspend fun credentialsFor(summary: AccountSummary): WebDavCredentials? {
        val password = credentialStore.loadPassword(summary.credentialRef) ?: return null
        return WebDavCredentials(
            baseUrl = summary.baseUrl,
            username = summary.username,
            password = password,
        )
    }

    /**
     * Persist a freshly tested account and mark it active.
     */
    suspend fun saveAndActivate(
        displayName: String,
        baseUrl: String,
        rootPath: String,
        username: String,
        password: String,
    ): AccountSummary {
        val now = clock()
        val credentialRef = CredentialStore.newCredentialRef()
        credentialStore.savePassword(credentialRef, password)
        accountDao.clearActive()
        val id = accountDao.insert(
            DavAccountEntity(
                displayName = displayName,
                baseUrl = baseUrl,
                rootPath = rootPath,
                username = username,
                credentialRef = credentialRef,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        return AccountSummary(
            id = id,
            displayName = displayName,
            baseUrl = baseUrl,
            rootPath = rootPath,
            username = username,
            credentialRef = credentialRef,
        )
    }
}

data class AccountSummary(
    val id: Long,
    val displayName: String,
    val baseUrl: String,
    val rootPath: String,
    val username: String,
    val credentialRef: String,
)
