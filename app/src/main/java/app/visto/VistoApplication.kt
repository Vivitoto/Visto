package app.visto

import android.app.Application
import app.visto.data.account.AccountService
import app.visto.data.account.CredentialStore
import app.visto.data.db.DavAccountDao
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.db.VistoDatabase

/**
 * Process-wide singletons. Visto v0.1 does not pull in Hilt to keep the
 * binary small and the build fast; this class is the central wiring point.
 */
class VistoApplication : Application() {

    lateinit var database: VistoDatabase
        private set

    val accountDao: DavAccountDao
        get() = database.davAccountDao()

    lateinit var remoteRepository: RemoteEntryRepository
        private set

    lateinit var credentialStore: CredentialStore
        private set

    lateinit var accountService: AccountService
        private set

    override fun onCreate() {
        super.onCreate()
        database = VistoDatabase.create(this)
        remoteRepository = RemoteEntryRepository(database, database.remoteEntryDao())
        credentialStore = CredentialStore(this)
        accountService = AccountService(accountDao, credentialStore)
    }
}
