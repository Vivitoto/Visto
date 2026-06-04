package app.visto.data.account

import androidx.room.withTransaction
import app.visto.data.db.DavAccountEntity
import app.visto.data.db.VistoDatabase
import app.visto.data.webdav.WebDavCredentials

/**
 * Coordinates [DavAccountEntity] persistence with [CredentialStore].
 *
 * Visto v0.1 manages a single active account, but the schema and this
 * service keep account ids first-class so additional accounts can be added
 * later without rewiring callers.
 */
class AccountService(
    private val database: VistoDatabase,
    private val credentialStore: CredentialStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val accountDao = database.davAccountDao()

    suspend fun activeAccount(): AccountSummary? {
        val entity = accountDao.getActive() ?: return null
        return entity.toSummary()
    }

    suspend fun listAll(): List<AccountSummary> = accountDao.getAll().map { it.toSummary() }

    suspend fun switchActive(id: Long): AccountSummary? {
        val target = accountDao.getById(id) ?: return null
        val now = clock()
        database.withTransaction {
            accountDao.clearActive()
            accountDao.markActive(id, now)
        }
        return target.toSummary().copy()
    }

    /**
     * Delete an account row and its stored password ref. Albums attached to
     * the account are cascaded by the schema; cached thumbnails on disk
     * stay because they are content-addressed by ETag/size, not account.
     */
    suspend fun deleteAccount(id: Long) {
        val entity = accountDao.getById(id) ?: return
        database.withTransaction {
            accountDao.deleteById(id)
            if (entity.isActive) {
                val replacement = accountDao.getAll().firstOrNull()
                if (replacement != null) {
                    accountDao.clearActive()
                    accountDao.markActive(replacement.id, clock())
                }
            }
        }
        credentialStore.deletePassword(entity.credentialRef)
    }

    private fun DavAccountEntity.toSummary(): AccountSummary = AccountSummary(
        id = id,
        displayName = displayName,
        baseUrl = baseUrl,
        rootPath = rootPath,
        username = username,
        credentialRef = credentialRef,
    )

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
     *
     * Re-saving the same baseUrl+username updates the existing row instead of
     * Room REPLACE-ing it. That preserves the account id and prevents
     * cascading deletion of saved albums/cache tied to the account.
     */
    suspend fun saveAndActivate(
        displayName: String,
        baseUrl: String,
        rootPath: String,
        username: String,
        password: String,
    ): AccountSummary {
        val now = clock()
        val newCredentialRef = CredentialStore.newCredentialRef()
        credentialStore.savePassword(newCredentialRef, password)
        var oldCredentialRefToDelete: String? = null
        return try {
            val summary = database.withTransaction {
                val existing = accountDao.getByBaseUrlAndUsername(baseUrl, username)
                accountDao.clearActive()
                val id = if (existing != null) {
                    oldCredentialRefToDelete = existing.credentialRef
                    accountDao.update(
                        existing.copy(
                            displayName = displayName,
                            rootPath = rootPath,
                            credentialRef = newCredentialRef,
                            isActive = true,
                            updatedAt = now,
                        )
                    )
                    existing.id
                } else {
                    accountDao.insert(
                        DavAccountEntity(
                            displayName = displayName,
                            baseUrl = baseUrl,
                            rootPath = rootPath,
                            username = username,
                            credentialRef = newCredentialRef,
                            isActive = true,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
                AccountSummary(
                    id = id,
                    displayName = displayName,
                    baseUrl = baseUrl,
                    rootPath = rootPath,
                    username = username,
                    credentialRef = newCredentialRef,
                )
            }
            oldCredentialRefToDelete
                ?.takeIf { it != newCredentialRef }
                ?.let { credentialStore.deletePassword(it) }
            summary
        } catch (t: Throwable) {
            credentialStore.deletePassword(newCredentialRef)
            throw t
        }
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
