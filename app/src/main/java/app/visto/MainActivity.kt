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
import app.visto.data.album.AlbumContents
import app.visto.data.album.AlbumLoader
import app.visto.data.db.AlbumSourceEntity
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import app.visto.ui.Strings
import app.visto.ui.account.AccountErrorMessages
import app.visto.ui.account.AccountFormReducer
import app.visto.ui.account.AccountFormState
import app.visto.ui.account.AccountScreen
import app.visto.ui.albums.AlbumAddFormReducer
import app.visto.ui.albums.AlbumAddFormState
import app.visto.ui.albums.AlbumAddValidator
import app.visto.ui.albums.AlbumDetailReducer
import app.visto.ui.albums.AlbumDetailScreen
import app.visto.ui.albums.AlbumDetailUiState
import app.visto.ui.albums.AlbumListScreen
import app.visto.ui.albums.AlbumListUiState
import app.visto.ui.albums.FolderPickerNavigator
import app.visto.ui.albums.FolderPickerScreen
import app.visto.ui.albums.FolderPickerState
import app.visto.ui.browser.BrowserNavigator
import app.visto.ui.browser.BrowserScreen
import app.visto.ui.browser.BrowserStateBuilder
import app.visto.ui.browser.BrowserUiState
import app.visto.ui.settings.BrowseMode
import app.visto.ui.settings.SettingsScreen
import app.visto.ui.settings.SettingsUiState
import app.visto.ui.viewer.ViewerScreen
import app.visto.ui.viewer.ViewerSession
import kotlinx.coroutines.Job
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
    var browseMode by remember { mutableStateOf(BrowseMode.ALBUMS) }

    LaunchedEffect(Unit) {
        val active = app.accountService.activeAccount()
        if (active != null) {
            val creds = app.accountService.credentialsFor(active)
            if (creds != null) {
                account = active
                credentials = creds
                screen = Screen.Home
            } else {
                screen = Screen.Account
            }
        } else {
            screen = Screen.Account
        }
    }

    when (screen) {
        Screen.Loading -> Text(text = Strings.LOADING)
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
                screen = Screen.Home
            },
            accountService = app.accountService,
        )
        Screen.Home -> {
            val summary = account
            val creds = credentials
            if (summary == null || creds == null) {
                screen = Screen.Account
            } else when (browseMode) {
                BrowseMode.ALBUMS -> AlbumsHost(
                    summary = summary,
                    credentials = creds,
                    browseMode = browseMode,
                    onBrowseModeChange = { browseMode = it },
                )
                BrowseMode.DIRECTORY -> BrowserHost(
                    summary = summary,
                    credentials = creds,
                    repository = app.remoteRepository,
                    browseMode = browseMode,
                    onBrowseModeChange = { browseMode = it },
                )
            }
        }
    }
}

private sealed interface Screen {
    data object Loading : Screen
    data object Account : Screen
    data object Home : Screen
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
                        Strings.ACCOUNT_CONNECTION_OK,
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
private fun AlbumsHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    browseMode: BrowseMode,
    onBrowseModeChange: (BrowseMode) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
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
    val albumLoader = remember(client) { AlbumLoader(client) }

    var listState by remember { mutableStateOf(AlbumListUiState()) }
    var openedAlbum by remember { mutableStateOf<AlbumSourceEntity?>(null) }
    var albumDetail by remember { mutableStateOf<AlbumDetailUiState?>(null) }
    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var folderPicker by remember { mutableStateOf<FolderPickerState?>(null) }
    var pendingDelete by remember { mutableStateOf<AlbumSourceEntity?>(null) }
    var activeLoadJob by remember { mutableStateOf<Job?>(null) }

    suspend fun refreshAlbumList() {
        listState = listState.copy(isLoading = true, errorMessage = null)
        try {
            val albums = app.database.albumSourceDao().listForAccount(summary.id)
            listState = listState.copy(isLoading = false, albums = albums)
        } catch (e: Throwable) {
            listState = listState.copy(isLoading = false, errorMessage = e.message ?: Strings.ERR_UNEXPECTED)
        }
    }

    LaunchedEffect(summary.id) { refreshAlbumList() }

    fun loadPickerPath(path: String) {
        val normalized = FolderPickerNavigator.normalize(path)
        folderPicker = FolderPickerState(currentPath = normalized, isLoading = true)
        scope.launch {
            try {
                val folders = client.listDirectory(normalized)
                    .filter { it.isDirectory }
                    .sortedBy { it.name.lowercase() }
                folderPicker = FolderPickerState(
                    currentPath = normalized,
                    folders = folders,
                    isLoading = false,
                )
            } catch (e: Throwable) {
                folderPicker = FolderPickerState(
                    currentPath = normalized,
                    folders = emptyList(),
                    isLoading = false,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    fun loadAlbum(target: AlbumSourceEntity) {
        activeLoadJob?.cancel()
        albumDetail = AlbumDetailUiState(
            title = target.displayName,
            rootPath = target.rootPath,
            isLoading = true,
        )
        activeLoadJob = scope.launch {
            try {
                albumLoader.load(target.rootPath).collect { contents: AlbumContents ->
                    albumDetail = AlbumDetailReducer.fromContents(
                        title = target.displayName,
                        contents = contents,
                        loading = true,
                    )
                }
                albumDetail = albumDetail?.copy(isLoading = false)
            } catch (e: Throwable) {
                albumDetail = albumDetail?.copy(
                    isLoading = false,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    // Viewer takes precedence.
    val activeSession = viewerSession
    if (activeSession != null) {
        ViewerScreen(
            session = activeSession,
            imageLoader = app.imageLoader,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            onClose = { viewerSession = null },
        )
        return
    }

    val picker = folderPicker
    if (picker != null) {
        FolderPickerScreen(
            state = picker,
            onBack = { folderPicker = null },
            onGoUp = { loadPickerPath(FolderPickerNavigator.parentOf(picker.currentPath)) },
            onOpenFolder = { folder -> loadPickerPath(folder.path) },
            onSelectCurrent = {
                listState = listState.copy(
                    addDialog = AlbumAddFormReducer.updatePath(listState.addDialog, picker.currentPath),
                )
                folderPicker = null
            },
        )
        return
    }

    if (showSettings) {
        SettingsHost(
            summary = summary,
            browseMode = browseMode,
            onBrowseModeChange = { mode ->
                onBrowseModeChange(mode)
                showSettings = false
            },
            onBack = { showSettings = false },
        )
        return
    }

    val opened = openedAlbum
    val detail = albumDetail
    if (opened != null && detail != null) {
        BackHandler {
            activeLoadJob?.cancel()
            openedAlbum = null
            albumDetail = null
        }
        AlbumDetailScreen(
            state = detail,
            imageLoader = app.imageLoader,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            onBack = {
                activeLoadJob?.cancel()
                openedAlbum = null
                albumDetail = null
            },
            onRefresh = { loadAlbum(opened) },
            onOpenMedia = { entry ->
                val flat = detail.flatMedia
                viewerSession = ViewerSession.build(flat, entry.path)
            },
        )
        return
    }

    AlbumListScreen(
        state = listState,
        onOpenAlbum = { album ->
            openedAlbum = album
            loadAlbum(album)
        },
        onAddRequested = {
            listState = listState.copy(showAddDialog = true, addDialog = AlbumAddFormReducer.reset())
        },
        onAddDismissed = {
            listState = listState.copy(showAddDialog = false, addDialog = AlbumAddFormReducer.reset())
        },
        onAddFormChange = { newForm ->
            listState = listState.copy(addDialog = newForm)
        },
        onAddSubmit = {
            val existing = listState.albums.map { it.rootPath }.toSet()
            when (val r = AlbumAddValidator.validate(listState.addDialog, existing)) {
                is AlbumAddValidator.Result.Err ->
                    listState = listState.copy(addDialog = AlbumAddFormReducer.setError(listState.addDialog, r.message))
                is AlbumAddValidator.Result.Ok -> {
                    listState = listState.copy(addDialog = AlbumAddFormReducer.setSaving(listState.addDialog, true))
                    scope.launch {
                        val now = System.currentTimeMillis()
                        try {
                            app.database.albumSourceDao().insert(
                                AlbumSourceEntity(
                                    accountId = summary.id,
                                    displayName = r.name,
                                    rootPath = r.path,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            listState = listState.copy(showAddDialog = false, addDialog = AlbumAddFormReducer.reset())
                            refreshAlbumList()
                        } catch (e: Throwable) {
                            listState = listState.copy(
                                addDialog = AlbumAddFormReducer.setError(
                                    listState.addDialog,
                                    e.message ?: Strings.ERR_UNEXPECTED,
                                )
                            )
                        }
                    }
                }
            }
        },
        onBrowsePathRequested = {
            val start = listState.addDialog.path.trim().ifEmpty { summary.rootPath }
            loadPickerPath(start)
        },
        onDeleteRequested = { album -> pendingDelete = album },
        onOpenSettings = { showSettings = true },
    )

    val toDelete = pendingDelete
    if (toDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(Strings.ALBUMS_DELETE_CONFIRM_TITLE) },
            text = { Text(Strings.ALBUMS_DELETE_CONFIRM_MESSAGE) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch {
                        app.database.albumSourceDao().deleteById(toDelete.id)
                        pendingDelete = null
                        refreshAlbumList()
                    }
                }) { Text(Strings.ALBUMS_DELETE) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text(Strings.ALBUMS_CANCEL) }
            },
        )
    }
}

@Composable
private fun SettingsHost(
    summary: AccountSummary,
    browseMode: BrowseMode,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val scope = rememberCoroutineScope()
    var settingsState by remember {
        mutableStateOf(
            SettingsUiState(
                accountDisplayName = summary.displayName,
                accountBaseUrl = summary.baseUrl,
                accountRoot = summary.rootPath,
                thumbnailCacheBytes = app.imageLoader.diskCache?.size ?: 0L,
            )
        )
    }
    SettingsScreen(
        state = settingsState,
        onBack = onBack,
        browseMode = browseMode,
        onBrowseModeChange = onBrowseModeChange,
        onClearCache = {
            scope.launch {
                settingsState = settingsState.copy(isClearingCache = true, message = null)
                app.imageLoader.memoryCache?.clear()
                app.imageLoader.diskCache?.clear()
                settingsState = settingsState.copy(
                    isClearingCache = false,
                    thumbnailCacheBytes = app.imageLoader.diskCache?.size ?: 0L,
                    message = Strings.SETTINGS_CACHE_CLEARED,
                )
            }
        },
    )
}

@Composable
private fun BrowserHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    repository: RemoteEntryRepository,
    browseMode: BrowseMode,
    onBrowseModeChange: (BrowseMode) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val navigator = remember(summary.id) { BrowserNavigator(summary.rootPath) }
    var uiState by remember { mutableStateOf(BrowserUiState(currentPath = summary.rootPath)) }
    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var showSettings by remember { mutableStateOf(false) }
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

    val activeSession = viewerSession
    if (activeSession != null) {
        ViewerScreen(
            session = activeSession,
            imageLoader = app.imageLoader,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            onClose = { viewerSession = null },
        )
        return
    }

    if (showSettings) {
        SettingsHost(
            summary = summary,
            browseMode = browseMode,
            onBrowseModeChange = { mode ->
                onBrowseModeChange(mode)
                showSettings = false
            },
            onBack = { showSettings = false },
        )
        return
    }

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
        onOpenMedia = { opened ->
            val all = uiState.media
            viewerSession = ViewerSession.build(all, opened.path)
        },
        onRefresh = { loadCurrent(forceRefresh = true) },
        onOpenSettings = { showSettings = true },
    )
}
