package app.visto

import android.app.Application
import app.visto.data.account.AccountService
import app.visto.data.account.CredentialStore
import app.visto.data.account.VistoPreferences
import app.visto.data.db.DavAccountDao
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.db.VistoDatabase
import app.visto.data.thumbnail.VistoImageLoaderFactory
import app.visto.data.webdav.WebDavAuthInterceptor
import coil.ImageLoader
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

    lateinit var authInterceptor: WebDavAuthInterceptor
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var imageLoader: ImageLoader
        private set

    lateinit var preferences: VistoPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = VistoDatabase.create(this)
        remoteRepository = RemoteEntryRepository(database, database.remoteEntryDao())
        credentialStore = CredentialStore(this)
        accountService = AccountService(accountDao, credentialStore)
        authInterceptor = WebDavAuthInterceptor()
        val dispatcher = Dispatcher().apply {
            // Be gentle with home NAS/WebDAV servers. Album scanning and
            // thumbnail loading can otherwise pile up too many simultaneous
            // requests against one host.
            maxRequests = 12
            maxRequestsPerHost = 6
        }
        okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)
            .build()
        imageLoader = VistoImageLoaderFactory.create(this, okHttpClient)
        preferences = VistoPreferences(this)
    }
}
