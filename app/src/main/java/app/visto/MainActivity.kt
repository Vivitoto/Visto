package app.visto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.visto.data.account.AccountService
import app.visto.data.account.AccountSummary
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import app.visto.ui.account.AccountErrorMessages
import app.visto.ui.account.AccountFormReducer
import app.visto.ui.account.AccountFormState
import app.visto.ui.account.AccountScreen
import app.visto.ui.browser.BrowserNavigator
import app.visto.ui.browser.BrowserScreen
import app.visto.ui.browser.BrowserStateBuilder
import app.visto.ui.browser.BrowserUiState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VistoRoot()
                }
            }
        }
    }
}

@Composable
fun VistoRoot() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }

    var screen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var account by remember { mutableStateOf<AccountSummary?>(null) }
    var credentials by remember { mutableStateOf<WebDavCredentials?>(null) }

    LaunchedEffect(Unit) {
        val active = app.accountService.activeAccount()
        if (active != null) {
            val creds = app.accountService.credentialsFor(active)
            if (creds != null) {
                account = active
                credentials = creds
                screen = Screen.Browser
            } else {
                screen = Screen.Account
            }
        } else {
            screen = Screen.Account
        }
    }

    when (val current = screen) {
        Screen.Loading -> Text(text = "Loading…")
        Screen.Account -> AccountSetup(
            initial = account?.let {
                AccountFormState(
                    displayName = it.displayName,
                    baseUrl = it.baseUrl,
                    username = it.username,
                    rootPath = it.rootPath,
                )
            } ?: AccountFormState(),
            onSaved = { savedSummary, savedCredentials ->
                account = savedSummary
                credentials = savedCredentials
                screen = Screen.Browser
            },
            accountService = app.accountService,
        )
        Screen.Browser -> {
            val summary = account
            val creds = credentials
            if (summary == null || creds == null) {
                screen = Screen.Account
            } else {
                BrowserHost(
                    summary = summary,
                    credentials = creds,
                    repository = app.remoteRepository,
                )
            }
        }
    }
}

private sealed interface Screen {
    data object Loading : Screen
    data object Account : Screen
    data object Browser : Screen
}

@Composable
private fun AccountSetup(
    initial: AccountFormState,
    accountService: AccountService,
    onSaved: (AccountSummary, WebDavCredentials) -> Unit,
) {
    var state by remember { mutableStateOf(initial) }
    val scope = rememberCoroutineScope()

    AccountScreen(
        state = state,
        onStateChange = { state = it },
        onTestConnection = {
            val current = state
            scope.launch {
                state = AccountFormReducer.setTesting(current, true)
                try {
                    val client = WebDavClient(
                        credentials = WebDavCredentials(
                            baseUrl = current.baseUrl.trim(),
                            username = current.username,
                            password = current.password,
                        ),
                        accountId = -1L,
                    )
                    client.listDirectory(current.normalizedRootPath)
                    state = AccountFormReducer.setMessage(
                        AccountFormReducer.setTesting(state, false),
                        "Connection succeeded.",
                    )
                } catch (e: Throwable) {
                    state = AccountFormReducer.setError(state, AccountErrorMessages.forWebDavError(e))
                }
            }
        },
        onSave = {
            val current = state
            scope.launch {
                state = AccountFormReducer.setSaving(current, true)
                try {
                    val savedSummary = accountService.saveAndActivate(
                        displayName = current.displayName,
                        baseUrl = current.baseUrl.trim(),
                        rootPath = current.normalizedRootPath,
                        username = current.username,
                        password = current.password,
                    )
                    val savedCredentials = WebDavCredentials(
                        baseUrl = savedSummary.baseUrl,
                        username = savedSummary.username,
                        password = current.password,
                    )
                    onSaved(savedSummary, savedCredentials)
                } catch (e: Throwable) {
                    state = AccountFormReducer.setError(state, AccountErrorMessages.forWebDavError(e))
                }
            }
        },
    )
}

@Composable
private fun BrowserHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    repository: RemoteEntryRepository,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val navigator = remember(summary.id) { BrowserNavigator(summary.rootPath) }
    var uiState by remember { mutableStateOf(BrowserUiState(currentPath = summary.rootPath)) }
    val scope = rememberCoroutineScope()
    val client = remember(summary.id) {
        app.authInterceptor.setAccount(
            baseUrl = credentials.baseUrl,
            username = credentials.username,
            password = credentials.password,
        )
        WebDavClient(
            credentials = credentials,
            accountId = summary.id,
            httpClient = app.okHttpClient,
        )
    }

    fun loadCurrent(forceRefresh: Boolean) {
        scope.launch {
            val currentPath = navigator.currentPath
            val cached = repository.entriesForParent(summary.id, currentPath)
            uiState = BrowserStateBuilder.apply(
                currentPath = currentPath,
                entries = cached,
                sortMode = uiState.sortMode,
                isLoading = cached.isEmpty(),
                isRefreshing = true,
            )
            try {
                val fresh = client.listDirectory(currentPath)
                repository.replaceDirectoryListing(summary.id, currentPath, fresh)
                uiState = BrowserStateBuilder.apply(
                    currentPath = currentPath,
                    entries = fresh,
                    sortMode = uiState.sortMode,
                )
            } catch (e: Throwable) {
                uiState = BrowserStateBuilder.apply(
                    currentPath = currentPath,
                    entries = cached,
                    sortMode = uiState.sortMode,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    LaunchedEffect(summary.id) { loadCurrent(forceRefresh = true) }

    BackHandler(enabled = navigator.canGoBack) {
        if (navigator.back() != null) loadCurrent(forceRefresh = false)
    }

    BrowserScreen(
        state = uiState,
        imageLoader = app.imageLoader,
        mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
        onBack = {
            if (navigator.back() != null) loadCurrent(forceRefresh = false)
        },
        onOpenFolder = { folder ->
            navigator.open(folder.path)
            loadCurrent(forceRefresh = false)
        },
        onOpenMedia = { /* Phase 7 will hook the viewer. */ },
        onRefresh = { loadCurrent(forceRefresh = true) },
        onOpenSettings = { /* Phase 8 will hook the settings screen. */ },
    )
}
