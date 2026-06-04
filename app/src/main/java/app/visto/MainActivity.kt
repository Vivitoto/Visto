package app.visto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.visto.data.account.AlbumViewMode
import app.visto.data.account.AccountService
import app.visto.data.account.AccountSummary
import app.visto.core.model.DavPath
import app.visto.data.album.AlbumContents
import app.visto.data.album.AlbumCoverFinder
import app.visto.data.album.AlbumLoader
import app.visto.data.db.AlbumSourceEntity
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import app.visto.ui.HomeTab
import app.visto.ui.Strings
import app.visto.ui.VistoBottomBar
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
import app.visto.ui.settings.SettingsScreen
import app.visto.ui.settings.SettingsUiState
import app.visto.ui.theme.ThemeMode
import app.visto.ui.theme.VistoTheme
import app.visto.ui.viewer.ViewerScreen
import app.visto.ui.viewer.ViewerSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as VistoApplication
            var themeMode by remember { mutableStateOf(app.preferences.themeMode) }
            VistoTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VistoRoot(
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            app.preferences.themeMode = it
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun VistoRoot(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }

    var screen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var account by remember { mutableStateOf<AccountSummary?>(null) }
    var credentials by remember { mutableStateOf<WebDavCredentials?>(null) }
    var selectedTab by remember { mutableStateOf(HomeTab.ALBUMS) }

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
        Screen.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = Strings.LOADING,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
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
            } else when (selectedTab) {
                HomeTab.ALBUMS -> AlbumsHost(
                    summary = summary,
                    credentials = creds,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
                HomeTab.BROWSER -> BrowserHost(
                    summary = summary,
                    credentials = creds,
                    repository = app.remoteRepository,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
                HomeTab.SETTINGS -> SettingsHost(
                    summary = summary,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
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
            if (!current.isSafeRootPath) {
                state = AccountFormReducer.setError(current, Strings.ERR_INVALID_PATH)
                return@AccountScreen
            }
            scope.launch {
                state = AccountFormReducer.setTesting(current, true)
                try {
                    val client = WebDavClient(
                        credentials = WebDavCredentials(
                            baseUrl = current.baseUrl.trim(),
                            username = current.username,
                            password = current.password,
                        ),
                        accountId = 0L,
                    )
                    client.listDirectory(current.normalizedRootPath)
                    state = AccountFormReducer.setMessage(
                        AccountFormReducer.setTesting(state, false),
                        Strings.ACCOUNT_CONNECTION_OK,
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Throwable) {
                    state = AccountFormReducer.setError(state, AccountErrorMessages.forWebDavError(e))
                }
            }
        },
        onSave = {
            val current = state
            if (!current.isSafeRootPath) {
                state = AccountFormReducer.setError(current, Strings.ERR_INVALID_PATH)
                return@AccountScreen
            }
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
                } catch (ce: CancellationException) {
                    throw ce
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
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
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
    val coverFinder = remember(client) { AlbumCoverFinder(client, maxCoverBytes = app.preferences.maxGridThumbnailBytes) }

    var listState by remember { mutableStateOf(AlbumListUiState()) }
    var openedAlbum by remember { mutableStateOf<AlbumSourceEntity?>(null) }
    var albumDetail by remember { mutableStateOf<AlbumDetailUiState?>(null) }
    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var folderPicker by remember { mutableStateOf<FolderPickerState?>(null) }
    var pendingDelete by remember { mutableStateOf<AlbumSourceEntity?>(null) }
    var activeLoadJob by remember { mutableStateOf<Job?>(null) }
    var activeLoadGeneration by remember { mutableStateOf(0) }
    var activeFolderPickerJob by remember { mutableStateOf<Job?>(null) }
    var activeFolderPickerGeneration by remember { mutableStateOf(0) }
    // Per-session in-memory cache of album cover image paths. Recomputed on
    // app restart so we don't need a schema migration just for first-run UI
    // polish. Value is null when no cover was found (negative cache).
    val albumCovers = remember { mutableStateMapOf<Long, String?>() }
    val coverLoadJobs = remember { mutableMapOf<Long, Job>() }

    suspend fun refreshAlbumList() {
        listState = listState.copy(isLoading = true, errorMessage = null)
        try {
            val albums = app.database.albumSourceDao().listForAccount(summary.id)
            listState = listState.copy(isLoading = false, albums = albums)
            // Kick off cover discovery for any album we haven't probed yet.
            for (album in albums) {
                if (!albumCovers.containsKey(album.id) && coverLoadJobs[album.id]?.isActive != true) {
                    coverLoadJobs[album.id] = scope.launch {
                        try {
                            val cover = coverFinder.findCoverImage(album.rootPath)
                            albumCovers[album.id] = cover?.path
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Throwable) {
                            // Network blip; leave entry missing so we retry on next refresh.
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            listState = listState.copy(isLoading = false, errorMessage = e.message ?: Strings.ERR_UNEXPECTED)
        }
    }

    LaunchedEffect(summary.id) { refreshAlbumList() }

    fun loadPickerPath(path: String) {
        val pickerRoot = DavPath.normalize(summary.rootPath)
        val normalized = FolderPickerNavigator.clampToRoot(path, pickerRoot)
        activeFolderPickerJob?.cancel()
        val generation = activeFolderPickerGeneration + 1
        activeFolderPickerGeneration = generation
        folderPicker = FolderPickerState(currentPath = normalized, rootPath = pickerRoot, isLoading = true)
        activeFolderPickerJob = scope.launch {
            try {
                val folders = client.listDirectory(normalized)
                    .filter { it.isDirectory }
                    .sortedBy { it.name.lowercase() }
                if (generation != activeFolderPickerGeneration || folderPicker?.currentPath != normalized) return@launch
                folderPicker = FolderPickerState(
                    currentPath = normalized,
                    rootPath = pickerRoot,
                    folders = folders,
                    isLoading = false,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                if (generation != activeFolderPickerGeneration || folderPicker?.currentPath != normalized) return@launch
                folderPicker = FolderPickerState(
                    currentPath = normalized,
                    rootPath = pickerRoot,
                    folders = emptyList(),
                    isLoading = false,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    fun loadFolder(target: AlbumSourceEntity, path: String) {
        activeLoadJob?.cancel()
        val generation = activeLoadGeneration + 1
        activeLoadGeneration = generation
        val normalizedPath = DavPath.normalize(path)
        val baseState = albumDetail ?: AlbumDetailUiState(
            title = target.displayName,
            rootPath = DavPath.normalize(target.rootPath),
        )
        albumDetail = AlbumDetailReducer.startFolder(baseState, normalizedPath)
        activeLoadJob = scope.launch {
            try {
                val entries = client.listDirectory(normalizedPath)
                val current = albumDetail
                if (generation != activeLoadGeneration || current?.folderView?.currentPath != normalizedPath) return@launch
                albumDetail = AlbumDetailReducer.applyFolderContents(current, normalizedPath, entries)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                val current = albumDetail ?: return@launch
                if (generation != activeLoadGeneration || current.folderView.currentPath != normalizedPath) return@launch
                albumDetail = AlbumDetailReducer.applyError(current, AccountErrorMessages.forWebDavError(e))
            }
        }
    }

    fun loadFlat(target: AlbumSourceEntity) {
        activeLoadJob?.cancel()
        val generation = activeLoadGeneration + 1
        activeLoadGeneration = generation
        val baseState = albumDetail ?: AlbumDetailUiState(
            title = target.displayName,
            rootPath = DavPath.normalize(target.rootPath),
        )
        albumDetail = AlbumDetailReducer.startFlat(baseState)
        activeLoadJob = scope.launch {
            try {
                albumLoader.load(target.rootPath).collect { contents: AlbumContents ->
                    if (generation != activeLoadGeneration) return@collect
                    val current = albumDetail ?: return@collect
                    albumDetail = AlbumDetailReducer.applyFlatContents(current, contents, stillLoading = true)
                }
                if (generation == activeLoadGeneration) {
                    albumDetail = albumDetail?.let {
                        it.copy(flatView = it.flatView.copy(isLoading = false))
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                if (generation != activeLoadGeneration) return@launch
                val current = albumDetail ?: return@launch
                albumDetail = AlbumDetailReducer.applyError(current, AccountErrorMessages.forWebDavError(e))
            }
        }
    }

    fun openAlbum(target: AlbumSourceEntity) {
        openedAlbum = target
        val initialMode = app.preferences.albumViewMode
        val rootPath = DavPath.normalize(target.rootPath)
        albumDetail = AlbumDetailUiState(
            title = target.displayName,
            rootPath = rootPath,
            viewMode = initialMode,
        )
        when (initialMode) {
            AlbumViewMode.FOLDERS -> loadFolder(target, rootPath)
            AlbumViewMode.FLAT -> loadFlat(target)
        }
    }

    // Viewer takes precedence.
    val activeSession = viewerSession
    if (activeSession != null) {
        ViewerScreen(
            session = activeSession,
            imageLoader = app.imageLoader,
            okHttpClient = app.okHttpClient,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            cacheKeyScope = mediaCacheKeyScope(summary),
            autoLoadOriginalImages = app.preferences.autoLoadOriginalImages,
            onClose = { viewerSession = null },
        )
        return
    }

    val picker = folderPicker
    if (picker != null) {
        FolderPickerScreen(
            state = picker,
            onBack = {
                activeFolderPickerJob?.cancel()
                folderPicker = null
            },
            onGoUp = { loadPickerPath(FolderPickerNavigator.parentOf(picker.currentPath, picker.rootPath)) },
            onOpenFolder = { folder -> loadPickerPath(folder.path) },
            onSelectCurrent = {
                listState = listState.copy(
                    addDialog = AlbumAddFormReducer.updatePath(listState.addDialog, picker.currentPath),
                )
                activeFolderPickerJob?.cancel()
                folderPicker = null
            },
            onRetry = { loadPickerPath(picker.currentPath) },
        )
        return
    }

    val opened = openedAlbum
    val detail = albumDetail
    if (opened != null && detail != null) {
        val rootPath = DavPath.normalize(opened.rootPath)
        val canGoUpInFolderMode = detail.viewMode == AlbumViewMode.FOLDERS &&
            detail.folderView.currentPath != rootPath
        BackHandler {
            if (canGoUpInFolderMode) {
                val parent = DavPath.parent(detail.folderView.currentPath) ?: rootPath
                val target = if (isAtOrBelowPath(parent, rootPath)) parent else rootPath
                loadFolder(opened, target)
            } else {
                activeLoadJob?.cancel()
                openedAlbum = null
                albumDetail = null
            }
        }
        AlbumDetailScreen(
            state = detail,
            albumRootPath = rootPath,
            imageLoader = app.imageLoader,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            mediaCacheKeyOf = { entry -> mediaCacheKeyScope(summary) + ":" + entry.path },
            maxGridThumbnailBytes = app.preferences.maxGridThumbnailBytes,
            onBack = {
                if (canGoUpInFolderMode) {
                    val parent = DavPath.parent(detail.folderView.currentPath) ?: rootPath
                    val target = if (isAtOrBelowPath(parent, rootPath)) parent else rootPath
                    loadFolder(opened, target)
                } else {
                    activeLoadJob?.cancel()
                    openedAlbum = null
                    albumDetail = null
                }
            },
            onRefresh = {
                when (detail.viewMode) {
                    AlbumViewMode.FOLDERS -> loadFolder(opened, detail.folderView.currentPath)
                    AlbumViewMode.FLAT -> loadFlat(opened)
                }
            },
            onOpenFolder = { folder -> loadFolder(opened, folder.path) },
            onOpenMedia = { entry ->
                viewerSession = ViewerSession.build(detail.visibleMedia, entry.path)
            },
            onSwitchToFolders = {
                app.preferences.albumViewMode = AlbumViewMode.FOLDERS
                loadFolder(opened, detail.folderView.currentPath.ifBlank { rootPath })
            },
            onSwitchToFlat = {
                app.preferences.albumViewMode = AlbumViewMode.FLAT
                loadFlat(opened)
            },
        )
        return
    }

    AlbumListScreen(
        state = listState,
        coverImagePathOf = { album -> albumCovers[album.id] },
        mediaUrlOf = { path -> client.mediaUrl(path) },
        mediaCacheKeyOf = { path -> mediaCacheKeyScope(summary) + ":" + path },
        imageLoader = app.imageLoader,
        onOpenAlbum = { album -> openAlbum(album) },
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
                        } catch (ce: CancellationException) {
                            throw ce
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
        onOpenSettings = { onTabSelected(HomeTab.SETTINGS) },
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
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
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val scope = rememberCoroutineScope()
    var settingsState by remember(themeMode) {
        mutableStateOf(
            SettingsUiState(
                accountDisplayName = summary.displayName,
                accountBaseUrl = summary.baseUrl,
                accountRoot = summary.rootPath,
                thumbnailCacheBytes = app.imageLoader.diskCache?.size ?: 0L,
                themeMode = themeMode,
                autoLoadOriginalImages = app.preferences.autoLoadOriginalImages,
            )
        )
    }
    SettingsScreen(
        state = settingsState,
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
        onThemeModeChange = onThemeModeChange,
        onAutoLoadOriginalImagesChange = { enabled ->
            app.preferences.autoLoadOriginalImages = enabled
            settingsState = settingsState.copy(autoLoadOriginalImages = enabled)
        },
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

private fun isAtOrBelowPath(path: String, root: String): Boolean {
    val normalizedPath = DavPath.normalize(path)
    val normalizedRoot = DavPath.normalize(root)
    if (normalizedRoot == DavPath.ROOT) return normalizedPath.startsWith(DavPath.ROOT)
    return normalizedPath == normalizedRoot || normalizedPath.startsWith("$normalizedRoot/")
}

private fun mediaCacheKeyScope(summary: AccountSummary): String =
    "account:${summary.id}:${summary.credentialRef}"

@Composable
private fun BrowserHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    repository: RemoteEntryRepository,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val navigator = remember(summary.id) { BrowserNavigator(summary.rootPath) }
    var uiState by remember { mutableStateOf(BrowserUiState(currentPath = summary.rootPath)) }
    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var activeBrowserLoadJob by remember { mutableStateOf<Job?>(null) }
    var activeBrowserLoadGeneration by remember { mutableStateOf(0) }
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
        activeBrowserLoadJob?.cancel()
        val generation = activeBrowserLoadGeneration + 1
        activeBrowserLoadGeneration = generation
        activeBrowserLoadJob = scope.launch {
            val currentPath = navigator.currentPath
            val cached = repository.entriesForParent(summary.id, currentPath)
            if (generation != activeBrowserLoadGeneration || navigator.currentPath != currentPath) return@launch
            uiState = BrowserStateBuilder.apply(
                currentPath = currentPath,
                entries = cached,
                sortMode = uiState.sortMode,
                isLoading = cached.isEmpty(),
                isRefreshing = forceRefresh || cached.isNotEmpty(),
            )
            try {
                val fresh = client.listDirectory(currentPath)
                repository.replaceDirectoryListing(summary.id, currentPath, fresh)
                if (generation != activeBrowserLoadGeneration || navigator.currentPath != currentPath) return@launch
                uiState = BrowserStateBuilder.apply(
                    currentPath = currentPath,
                    entries = fresh,
                    sortMode = uiState.sortMode,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                if (generation != activeBrowserLoadGeneration || navigator.currentPath != currentPath) return@launch
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
            okHttpClient = app.okHttpClient,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            cacheKeyScope = mediaCacheKeyScope(summary),
            autoLoadOriginalImages = app.preferences.autoLoadOriginalImages,
            onClose = { viewerSession = null },
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
        mediaCacheKeyOf = { entry -> mediaCacheKeyScope(summary) + ":" + entry.path },
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
        onOpenSettings = { onTabSelected(HomeTab.SETTINGS) },
        maxGridThumbnailBytes = app.preferences.maxGridThumbnailBytes,
        canGoBack = navigator.canGoBack,
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
    )
}
