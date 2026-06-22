package app.visto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.visto.data.account.AlbumViewMode
import app.visto.data.account.AccountService
import app.visto.data.account.AccountSummary
import app.visto.data.account.GridDensity
import app.visto.data.account.ReaderDefaultSettings
import app.visto.core.book.BookTextLoader
import app.visto.core.book.ChapterParser
import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import app.visto.data.album.AlbumPreviewFinder
import app.visto.data.album.AlbumCoverFinder
import app.visto.data.cache.AlbumIndexCache
import app.visto.data.db.AlbumSourceEntity
import app.visto.data.db.BookProgressEntity
import app.visto.data.db.RemoteEntryRepository
import app.visto.data.thumbnail.AnimatedThumbnailCache
import app.visto.data.thumbnail.GeneratedThumbnailCache
import app.visto.data.thumbnail.ThumbnailCacheKey
import app.visto.data.update.AppUpdateService
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials
import app.visto.data.webdav.WebDavDiagnosticResult
import app.visto.data.webdav.WebDavDiagnosticStatus
import app.visto.data.webdav.WebDavDiagnosticStep
import app.visto.data.webdav.WebDavDiagnosticsService
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
import app.visto.ui.bookshelf.BookshelfScreen
import app.visto.ui.bookshelf.BookshelfStateBuilder
import app.visto.ui.bookshelf.BookshelfUiState
import app.visto.ui.browser.BrowserNavigator
import app.visto.ui.browser.BrowserScreen
import app.visto.ui.browser.BrowserStateBuilder
import app.visto.ui.browser.BrowserUiState
import app.visto.ui.reader.ReaderAction
import app.visto.ui.reader.ReaderFontChoice
import app.visto.ui.reader.ReaderReducer
import app.visto.ui.reader.ReaderScreen
import app.visto.ui.reader.ReaderSession
import app.visto.ui.reader.ReaderSettingsSheet
import app.visto.ui.reader.ReaderTheme
import app.visto.ui.reader.readerFontDirectory
import app.visto.ui.settings.SettingsScreen
import app.visto.ui.settings.SettingsUiState
import app.visto.ui.theme.ThemeMode
import app.visto.ui.theme.VistoTheme
import app.visto.ui.viewer.ViewerScreen
import app.visto.ui.viewer.ViewerSession
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as VistoApplication
            var themeMode by remember { mutableStateOf(app.preferences.themeMode) }
            var gridDensity by remember { mutableStateOf(app.preferences.gridDensity) }
            VistoTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VistoRoot(
                        themeMode = themeMode,
                        gridDensity = gridDensity,
                        onThemeModeChange = {
                            themeMode = it
                            app.preferences.themeMode = it
                        },
                        onGridDensityChange = {
                            gridDensity = it
                            app.preferences.gridDensity = it
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
    gridDensity: GridDensity,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }

    var screen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var account by remember { mutableStateOf<AccountSummary?>(null) }
    var accounts by remember { mutableStateOf<List<AccountSummary>>(emptyList()) }
    var credentials by remember { mutableStateOf<WebDavCredentials?>(null) }
    var selectedTab by remember { mutableStateOf(HomeTab.ALBUMS) }
    val rememberedBrowserPaths = remember { mutableStateMapOf<Long, String>() }
    val rootScope = rememberCoroutineScope()

    suspend fun refreshAccounts() {
        accounts = app.accountService.listAll()
        val active = app.accountService.activeAccount()
        account = active
        credentials = active?.let { app.accountService.credentialsFor(it) }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
        screen = Screen.Home
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
            initial = AccountFormState(),
            onSaved = { savedSummary, savedCredentials ->
                account = savedSummary
                credentials = savedCredentials
                accounts = accounts.filterNot { it.id == savedSummary.id } + savedSummary
                screen = Screen.Home
            },
            accountService = app.accountService,
            onCancel = { screen = Screen.Home },
        )
        Screen.Home -> {
            val summary = account
            val creds = credentials
            if (summary == null || creds == null) when (selectedTab) {
                HomeTab.ALBUMS, HomeTab.BOOKSHELF, HomeTab.BROWSER -> NoAccountHome(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddServer = { screen = Screen.Account },
                )
                HomeTab.SETTINGS -> SettingsHost(
                    summary = null,
                    accounts = accounts,
                    themeMode = themeMode,
                    gridDensity = gridDensity,
                    onThemeModeChange = onThemeModeChange,
                    onGridDensityChange = onGridDensityChange,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddServer = { screen = Screen.Account },
                    onSwitchAccount = { id ->
                        rootScope.launch {
                            withContext(Dispatchers.IO) { clearAllBookCache(context.cacheDir) }
                            app.accountService.switchActive(id)
                            refreshAccounts()
                        }
                    },
                    onDeleteAccount = { id ->
                        rootScope.launch {
                            app.accountService.deleteAccount(id)
                            withContext(Dispatchers.IO) { clearAllBookCache(context.cacheDir) }
                            refreshAccounts()
                        }
                    },
                )
            } else when (selectedTab) {
                HomeTab.ALBUMS -> AlbumsHost(
                    summary = summary,
                    credentials = creds,
                    gridDensity = gridDensity,
                    onGridDensityChange = onGridDensityChange,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
                HomeTab.BOOKSHELF -> BookshelfHost(
                    summary = summary,
                    onTabSelected = { selectedTab = it },
                )
                HomeTab.BROWSER -> BrowserHost(
                    summary = summary,
                    credentials = creds,
                    repository = app.remoteRepository,
                    initialPath = rememberedBrowserPaths[summary.id]?.takeIf { isAtOrBelowPath(it, summary.rootPath) } ?: summary.rootPath,
                    onPathChanged = { rememberedBrowserPaths[summary.id] = it },
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
                HomeTab.SETTINGS -> SettingsHost(
                    summary = summary,
                    accounts = accounts,
                    themeMode = themeMode,
                    gridDensity = gridDensity,
                    onThemeModeChange = onThemeModeChange,
                    onGridDensityChange = onGridDensityChange,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddServer = { screen = Screen.Account },
                    onSwitchAccount = { id ->
                        rootScope.launch {
                            withContext(Dispatchers.IO) { clearAllBookCache(context.cacheDir) }
                            app.accountService.switchActive(id)
                            refreshAccounts()
                        }
                    },
                    onDeleteAccount = { id ->
                        rootScope.launch {
                            app.accountService.deleteAccount(id)
                            withContext(Dispatchers.IO) { clearAllBookCache(context.cacheDir) }
                            refreshAccounts()
                        }
                    },
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
private fun ActiveReaderScreen(
    session: ReaderSession,
    onSessionChange: (ReaderSession) -> Unit,
    onClose: () -> Unit,
    onPersistProgress: (ReaderSession) -> Unit,
    onSetDefaultSettings: (ReaderSession) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestSession by rememberUpdatedState(session)
    var showSettings by remember(session.filePath) { mutableStateOf(false) }
    var fontImportError by remember(session.filePath) { mutableStateOf<String?>(null) }

    fun updateSession(action: ReaderAction, persist: Boolean = true) {
        val base = latestSession
        val updated = ReaderReducer.reduce(base, action)
        if (updated == base) return
        onSessionChange(updated)
        if (persist) {
            onPersistProgress(updated)
        }
    }

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = try {
                withContext(Dispatchers.IO) { importReaderFontFromUri(context, uri) }
            } catch (_: UnsupportedReaderFontException) {
                fontImportError = Strings.READER_FONT_IMPORT_UNSUPPORTED
                return@launch
            } catch (_: Throwable) {
                fontImportError = Strings.READER_FONT_IMPORT_FAILED
                return@launch
            }
            fontImportError = null
            updateSession(ReaderAction.SetFontChoice(imported))
        }
    }

    ReaderScreen(
        session = session,
        onBack = onClose,
        onChapterSelect = { updateSession(ReaderAction.GoToChapter(it)) },
        onSettingsToggle = { showSettings = true },
        onSaveProgress = { saved ->
            onSessionChange(saved)
            onPersistProgress(saved)
        },
        onViewportChange = { viewport ->
            updateSession(ReaderAction.SetViewport(viewport), persist = false)
        },
    )

    if (showSettings) {
        ReaderSettingsSheet(
            current = session,
            onFontSize = { updateSession(ReaderAction.SetFontSize(it)) },
            onLineSpacing = { updateSession(ReaderAction.SetLineSpacing(it)) },
            onFontChoice = {
                fontImportError = null
                updateSession(ReaderAction.SetFontChoice(it))
            },
            onImportFont = {
                fontImportError = null
                fontPicker.launch(READER_FONT_PICKER_MIME_TYPES)
            },
            onTheme = { updateSession(ReaderAction.SetTheme(it)) },
            onSetDefaultSettings = { onSetDefaultSettings(latestSession) },
            onDismiss = { showSettings = false },
            fontImportError = fontImportError,
        )
    }
}

private fun readerLoadingSession(
    filePath: String,
    fileName: String,
    progress: BookProgressEntity? = null,
    defaultSettings: ReaderDefaultSettings = ReaderDefaultSettings(),
): ReaderSession = ReaderSession(
    filePath = filePath,
    fileName = fileName,
    encoding = progress?.encoding ?: "UTF-8",
    fullText = "",
    chapters = emptyList(),
    currentChapterIndex = progress?.chapterIndex ?: 0,
    currentPage = progress?.pageOffset ?: 0,
    pagesForCurrentChapter = emptyList(),
    fontSizeSp = progress?.fontSizeSp ?: defaultSettings.fontSizeSp,
    lineSpacing = progress?.lineSpacing ?: defaultSettings.lineSpacing,
    fontChoice = ReaderFontChoice.fromStorage(progress?.fontChoice ?: defaultSettings.fontChoice),
    theme = (progress?.theme ?: defaultSettings.theme).toReaderTheme(),
    isLoading = true,
    errorMessage = null,
)

private fun String?.toReaderTheme(): ReaderTheme = when (this?.lowercase()) {
    "dark" -> ReaderTheme.DARK
    "cream" -> ReaderTheme.CREAM
    else -> ReaderTheme.LIGHT
}

private fun ReaderTheme.toProgressTheme(): String = when (this) {
    ReaderTheme.LIGHT -> "light"
    ReaderTheme.DARK -> "dark"
    ReaderTheme.CREAM -> "cream"
}

private fun ReaderSession.toDefaultSettings(): ReaderDefaultSettings = ReaderDefaultSettings(
    fontSizeSp = fontSizeSp,
    lineSpacing = lineSpacing,
    theme = theme.toProgressTheme(),
    fontChoice = fontChoice.storageKey,
)

private suspend fun saveBookProgress(
    entity: BookProgressEntity?,
    session: ReaderSession,
    accountId: Long,
    sizeBytes: Long?,
    etag: String?,
    upsert: (BookProgressEntity) -> Unit,
) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val chapter = session.chapters.getOrNull(session.currentChapterIndex)
    upsert(
        BookProgressEntity(
            id = entity?.id ?: 0,
            accountId = accountId,
            path = session.filePath,
            name = session.fileName,
            sizeBytes = sizeBytes ?: entity?.sizeBytes,
            etag = etag,
            encoding = session.encoding.ifBlank { entity?.encoding ?: "UTF-8" },
            chapterIndex = session.currentChapterIndex,
            chapterTitle = chapter?.title,
            pageOffset = session.currentPage,
            totalChapters = session.chapters.size,
            fontSizeSp = session.fontSizeSp,
            lineSpacing = session.lineSpacing,
            theme = session.theme.toProgressTheme(),
            fontChoice = session.fontChoice.storageKey,
            lastReadAt = now,
            addedAt = entity?.addedAt ?: now,
        )
    )
}

private val READER_FONT_PICKER_MIME_TYPES = arrayOf(
    "font/ttf",
    "font/otf",
    "application/x-font-ttf",
    "application/x-font-otf",
    "application/octet-stream",
    "*/*",
)

private class UnsupportedReaderFontException : Exception()

private fun importReaderFontFromUri(context: android.content.Context, uri: Uri): ReaderFontChoice.Custom {
    val displayName = context.readerFontDisplayName(uri)
    val targetName = importedReaderFontFileName(displayName, System.currentTimeMillis())
        ?: throw UnsupportedReaderFontException()
    val targetDir = readerFontDirectory(context).apply { mkdirs() }
    val targetFile = File(targetDir, targetName)

    val resolver = context.contentResolver
    resolver.openInputStream(uri)?.use { input ->
        targetFile.outputStream().use { output -> input.copyTo(output) }
    } ?: throw IOException("Unable to open reader font input stream")

    return ReaderFontChoice.Custom(targetFile.name)
}

private fun android.content.Context.readerFontDisplayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                val name = cursor.getString(index)
                if (!name.isNullOrBlank()) return name
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "reader-font.ttf"
}

internal fun importedReaderFontFileName(displayName: String, nowMillis: Long): String? {
    val trimmed = displayName.trim()
    val extension = trimmed.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it == "ttf" || it == "otf" }
        ?: return null
    val base = trimmed
        .substringBeforeLast('.', missingDelimiterValue = "font")
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_', '.', '-')
        .take(48)
        .ifBlank { "font" }
    return "${nowMillis}_${base}.$extension"
}

@Composable
private fun NoAccountHome(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onAddServer: () -> Unit,
) {
    Scaffold(
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = Strings.NO_SERVER_TITLE,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = Strings.NO_SERVER_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onAddServer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Text(Strings.SETTINGS_ADD_SERVER)
            }
        }
    }
}

@Composable
private fun AccountSetup(
    initial: AccountFormState,
    accountService: AccountService,
    onSaved: (AccountSummary, WebDavCredentials) -> Unit,
    onCancel: () -> Unit,
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
                    val result = WebDavDiagnosticsService().diagnose(
                        credentials = WebDavCredentials(
                            baseUrl = current.baseUrl.trim(),
                            username = current.username,
                            password = current.password,
                        ),
                        accountId = 0L,
                        rootPath = current.normalizedRootPath,
                    )
                    state = AccountFormReducer.setDiagnostic(state, result)
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
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumsHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    gridDensity: GridDensity,
    onGridDensityChange: (GridDensity) -> Unit,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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
    val coverFinder = remember(client) { AlbumCoverFinder(client) }
    val previewFinder = remember(client) { AlbumPreviewFinder(client) }

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
    val albumPreviews = remember { mutableStateMapOf<Long, List<String>>() }
    val coverLoadJobs = remember { mutableMapOf<Long, Job>() }
    // Per-folder cover map: key is the folder's full WebDAV path, value is
    // the path of a representative image inside it, or null if no image
    // was found / probe failed. Used by FLAT icon grid to render folder
    // thumbnails instead of a blank folder icon.
    val folderPreviews = remember { mutableStateMapOf<String, List<String>>() }
    val folderCoverJobs = remember { mutableMapOf<String, Job>() }

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
                            val previews = previewFinder.findPreviewImages(album.rootPath, targetCount = 4)
                            if (previews.isNotEmpty()) {
                                albumPreviews[album.id] = previews.map { it.path }
                                albumCovers[album.id] = previews.first().path
                            } else {
                                // No images at the album level; fall back to
                                // the original deeper cover probe so the card
                                // can at least show one thumbnail.
                                val cover = coverFinder.findCoverImage(album.rootPath)
                                albumCovers[album.id] = cover?.path
                                cover?.let { albumPreviews[album.id] = listOf(it.path) }
                            }
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

    fun loadFolder(
        target: AlbumSourceEntity,
        path: String,
        viewMode: AlbumViewMode = AlbumViewMode.FOLDERS,
    ) {
        activeLoadJob?.cancel()
        val generation = activeLoadGeneration + 1
        activeLoadGeneration = generation
        val normalizedPath = DavPath.normalize(path)
        val baseState = albumDetail ?: AlbumDetailUiState(
            title = target.displayName,
            rootPath = DavPath.normalize(target.rootPath),
            viewMode = viewMode,
        )
        val cached = AlbumIndexCache.load(context, target.accountId, normalizedPath)
        albumDetail = if (cached != null) {
            AlbumDetailReducer.applyFolderContents(
                AlbumDetailReducer.startFolder(baseState, normalizedPath, viewMode),
                normalizedPath, cached
            )
        } else {
            AlbumDetailReducer.startFolder(baseState, normalizedPath, viewMode)
        }
        activeLoadJob = scope.launch {
            try {
                val entries = client.listDirectory(normalizedPath)
                AlbumIndexCache.save(context, target.accountId, normalizedPath, entries)
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

    fun loadIconGrid(target: AlbumSourceEntity, path: String) {
        activeLoadJob?.cancel()
        val generation = activeLoadGeneration + 1
        activeLoadGeneration = generation
        val normalizedPath = DavPath.normalize(path)
        val baseState = albumDetail ?: AlbumDetailUiState(
            title = target.displayName,
            rootPath = DavPath.normalize(target.rootPath),
            viewMode = AlbumViewMode.FLAT,
        )
        val cached = AlbumIndexCache.load(context, target.accountId, normalizedPath)
        albumDetail = if (cached != null) {
            AlbumDetailReducer.applyFolderContents(
                AlbumDetailReducer.startFolder(baseState, normalizedPath, AlbumViewMode.FLAT),
                normalizedPath, cached
            )
        } else {
            AlbumDetailReducer.startFolder(baseState, normalizedPath, AlbumViewMode.FLAT)
        }
        activeLoadJob = scope.launch {
            try {
                val entries = client.listDirectory(normalizedPath)
                AlbumIndexCache.save(context, target.accountId, normalizedPath, entries)
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

    fun openAlbum(target: AlbumSourceEntity) {
        openedAlbum = target
        val initialMode = app.preferences.albumViewMode
        val initialSortMode = app.preferences.albumSortMode
        val rootPath = DavPath.normalize(target.rootPath)
        albumDetail = AlbumDetailUiState(
            title = target.displayName,
            rootPath = rootPath,
            viewMode = initialMode,
            sortMode = initialSortMode,
        )
        when (initialMode) {
            AlbumViewMode.FOLDERS -> loadFolder(target, rootPath)
            AlbumViewMode.FLAT -> loadIconGrid(target, rootPath)
        }
    }

    // Viewer takes precedence.
    val activeSession = viewerSession
    if (activeSession != null) {
        BackHandler { viewerSession = null }
        ViewerScreen(
            session = activeSession,
            imageLoader = app.imageLoader,
            okHttpClient = app.okHttpClient,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            cacheKeyScope = mediaCacheKeyScope(summary),
            thumbnailCacheLimitBytes = app.preferences.thumbnailCacheLimit.bytes,
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
        val canGoUpInAlbum = detail.folderView.currentPath != rootPath
        BackHandler {
            if (canGoUpInAlbum) {
                val parent = DavPath.parent(detail.folderView.currentPath) ?: rootPath
                val target = if (isAtOrBelowPath(parent, rootPath)) parent else rootPath
                when (detail.viewMode) {
                    AlbumViewMode.FOLDERS -> loadFolder(opened, target)
                    AlbumViewMode.FLAT -> loadIconGrid(opened, target)
                }
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
            okHttpClient = app.okHttpClient,
            blurThumbnails = app.preferences.blurThumbnails,
            gridDensity = gridDensity,
            thumbnailCacheLimitBytes = app.preferences.thumbnailCacheLimit.bytes,
            onGridDensityChange = onGridDensityChange,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            mediaCacheKeyOf = { entry -> mediaCacheKeyScope(summary) + ":" + ThumbnailCacheKey.forEntry(entry) },
            folderPreviewPathsOf = { folder ->
                val key = folder.path
                val cached = folderPreviews[key]
                if (cached == null && folderCoverJobs[key]?.isActive != true && !folderPreviews.containsKey(key)) {
                    folderCoverJobs[key] = scope.launch {
                        try {
                            val previews = previewFinder
                                .findPreviewImages(folder.path, targetCount = 4)
                                .map { it.path }
                            folderPreviews[key] = previews
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Throwable) {
                            // Leave entry missing so the next refresh retries.
                        }
                    }
                }
                cached.orEmpty()
            },
            mediaUrlOfPath = { path -> client.mediaUrl(path) },
            mediaCacheKeyOfPath = { path -> mediaCacheKeyScope(summary) + ":" + path },
            sortMode = detail.sortMode,
            onSortModeChange = { mode ->
                app.preferences.albumSortMode = mode
                albumDetail = AlbumDetailReducer.applySort(detail, mode)
            },
            onBack = {
                if (canGoUpInAlbum) {
                    val parent = DavPath.parent(detail.folderView.currentPath) ?: rootPath
                    val target = if (isAtOrBelowPath(parent, rootPath)) parent else rootPath
                    when (detail.viewMode) {
                        AlbumViewMode.FOLDERS -> loadFolder(opened, target)
                        AlbumViewMode.FLAT -> loadIconGrid(opened, target)
                    }
                } else {
                    activeLoadJob?.cancel()
                    openedAlbum = null
                    albumDetail = null
                }
            },
            onRefresh = {
                when (detail.viewMode) {
                    AlbumViewMode.FOLDERS -> loadFolder(opened, detail.folderView.currentPath)
                    AlbumViewMode.FLAT -> loadIconGrid(opened, detail.folderView.currentPath)
                }
            },
            onOpenFolder = { folder ->
                when (detail.viewMode) {
                    AlbumViewMode.FOLDERS -> loadFolder(opened, folder.path)
                    AlbumViewMode.FLAT -> loadIconGrid(opened, folder.path)
                }
            },
            onOpenMedia = { entry ->
                viewerSession = ViewerSession.build(detail.visibleMedia, entry.path)
            },
            onSwitchToFolders = {
                app.preferences.albumViewMode = AlbumViewMode.FOLDERS
                loadFolder(opened, detail.folderView.currentPath.ifBlank { rootPath }, AlbumViewMode.FOLDERS)
            },
            onSwitchToFlat = {
                app.preferences.albumViewMode = AlbumViewMode.FLAT
                loadIconGrid(opened, detail.folderView.currentPath.ifBlank { rootPath })
            },
        )
        return
    }

    AlbumListScreen(
        state = listState,
        coverImagePathOf = { album -> albumCovers[album.id] },
        coverPreviewsOf = { album -> albumPreviews[album.id].orEmpty() },
        mediaUrlOf = { path -> client.mediaUrl(path) },
        mediaCacheKeyOf = { path -> mediaCacheKeyScope(summary) + ":" + path },
        imageLoader = app.imageLoader,
        blurThumbnails = app.preferences.blurThumbnails,
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
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
    )

    val toDelete = pendingDelete
    if (toDelete != null) {
        ModalBottomSheet(onDismissRequest = { pendingDelete = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = toDelete.displayName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = toDelete.rootPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        pendingDelete = null
                        openAlbum(toDelete)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.ALBUMS_OPEN) }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(toDelete.rootPath))
                        pendingDelete = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.ALBUMS_COPY_PATH) }
                Text(
                    text = Strings.ALBUMS_REMOVE_LOCAL_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(
                    onClick = {
                        scope.launch {
                            app.database.albumSourceDao().deleteById(toDelete.id)
                            pendingDelete = null
                            refreshAlbumList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.ALBUMS_REMOVE_LOCAL, color = MaterialTheme.colorScheme.error) }
                androidx.compose.material3.TextButton(
                    onClick = { pendingDelete = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.ALBUMS_CANCEL) }
            }
        }
    }
}

@Composable
private fun BookshelfHost(
    summary: AccountSummary,
    onTabSelected: (HomeTab) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val scope = rememberCoroutineScope()
    val dao = remember(app) { app.database.bookProgressDao() }
    val state by remember(summary.id) {
        BookshelfStateBuilder.fromFlow(dao.getAllByAccount(summary.id))
    }.collectAsState(initial = BookshelfUiState())
    var readerSession by remember(summary.id) { mutableStateOf<ReaderSession?>(null) }
    var activeReaderSizeBytes by remember { mutableStateOf<Long?>(null) }
    var activeReaderEtag by remember { mutableStateOf<String?>(null) }
    var activeReaderJob by remember { mutableStateOf<Job?>(null) }

    fun persistProgress(session: ReaderSession) {
        scope.launch {
            val existing = withContext(Dispatchers.IO) { dao.getByPath(summary.id, session.filePath) }
            saveBookProgress(
                entity = existing,
                session = session,
                accountId = summary.id,
                sizeBytes = activeReaderSizeBytes,
                etag = activeReaderEtag,
                upsert = dao::upsert,
            )
        }
    }

    fun openBook(book: BookProgressEntity) {
        activeReaderJob?.cancel()
        activeReaderSizeBytes = book.sizeBytes
        activeReaderEtag = book.etag
        readerSession = readerLoadingSession(
            filePath = book.path,
            fileName = book.name,
            progress = book,
        )
        activeReaderJob = scope.launch {
            try {
                val creds = app.accountService.credentialsFor(summary)
                    ?: error(Strings.ACCOUNT_CREDENTIALS_UNAVAILABLE)
                val client = WebDavClient(
                    credentials = creds,
                    accountId = summary.id,
                    httpClient = app.okHttpClient,
                )
                val result = BookTextLoader.load(client, book.path, context.cacheDir, expectedEtag = book.etag)
                val chapters = ChapterParser.parse(result.text)
                activeReaderSizeBytes = result.sizeBytes
                activeReaderEtag = result.etag
                val loaded = ReaderReducer.reduce(
                    readerSession ?: readerLoadingSession(book.path, book.name, book),
                    ReaderAction.Loaded(
                        encoding = result.encoding,
                        fullText = result.text,
                        chapters = chapters,
                        currentChapterIndex = book.chapterIndex,
                        currentPage = book.pageOffset,
                    ),
                )
                readerSession = loaded
                persistProgress(loaded)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                readerSession = (readerSession ?: readerLoadingSession(book.path, book.name, book)).copy(
                    isLoading = false,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    val activeReader = readerSession
    if (activeReader != null) {
        ActiveReaderScreen(
            session = activeReader,
            onSessionChange = { readerSession = it },
            onClose = {
                activeReaderJob?.cancel()
                activeReaderJob = null
                readerSession = null
            },
            onPersistProgress = ::persistProgress,
            onSetDefaultSettings = { app.preferences.defaultReaderSettings = it.toDefaultSettings() },
        )
        return
    }

    BookshelfScreen(
        state = state,
        onOpenBook = ::openBook,
        onRemoveBook = { book ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    dao.delete(summary.id, book.path)
                    clearBookCache(context.cacheDir, summary.id, book.path)
                }
            }
        },
        onTabSelected = onTabSelected,
    )
}

private fun clearBookCache(cacheDir: File, accountId: Long, path: String) {
    val booksRoot = File(cacheDir, "books")
    val pathDigest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(path.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    val accountDir = File(booksRoot, accountId.toString())
    accountDir.walkTopDown()
        .filter { it.isFile && it.name.contains(pathDigest) }
        .forEach { runCatching { it.delete() } }
    accountDir.takeIf { it.isDirectory && it.list().isNullOrEmpty() }?.delete()

    // Remove legacy path-only cache files written directly under books/.
    booksRoot.listFiles()
        ?.filter { it.isFile && it.name.contains(pathDigest) }
        ?.forEach { runCatching { it.delete() } }
}

private fun clearAllBookCache(cacheDir: File) {
    File(cacheDir, "books").deleteRecursively()
}

@Composable
private fun SettingsHost(
    summary: AccountSummary?,
    accounts: List<AccountSummary>,
    themeMode: ThemeMode,
    gridDensity: GridDensity,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onAddServer: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val updateService = remember(app) { AppUpdateService(app, app.okHttpClient) }
    val scope = rememberCoroutineScope()
    var settingsState by remember(summary?.id) {
        mutableStateOf(
            SettingsUiState(
                activeAccountId = summary?.id,
                accounts = accounts,
                accountDisplayName = summary?.displayName.orEmpty(),
                accountBaseUrl = summary?.baseUrl.orEmpty(),
                accountRoot = summary?.rootPath.orEmpty(),
                thumbnailCacheBytes = thumbnailCacheBytes(app),
                themeMode = themeMode,
                autoLoadOriginalImages = app.preferences.autoLoadOriginalImages,
                blurThumbnails = app.preferences.blurThumbnails,
                gridDensity = gridDensity,
                thumbnailCacheLimit = app.preferences.thumbnailCacheLimit,
            )
        )
    }
    SettingsScreen(
        state = settingsState.copy(
            activeAccountId = summary?.id,
            accounts = accounts,
            accountDisplayName = summary?.displayName.orEmpty(),
            accountBaseUrl = summary?.baseUrl.orEmpty(),
            accountRoot = summary?.rootPath.orEmpty(),
            themeMode = themeMode,
            gridDensity = gridDensity,
        ),
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
        onThemeModeChange = { mode ->
            settingsState = settingsState.copy(themeMode = mode)
            onThemeModeChange(mode)
        },
        onAddServer = onAddServer,
        onSwitchAccount = onSwitchAccount,
        onDeleteAccount = onDeleteAccount,
        onTestActiveConnection = {
            val active = summary ?: return@SettingsScreen
            scope.launch {
                settingsState = settingsState.copy(isTestingConnection = true, diagnostic = null)
                try {
                    val creds = app.accountService.credentialsFor(active)
                    if (creds == null) {
                        settingsState = settingsState.copy(
                            isTestingConnection = false,
                            diagnostic = WebDavDiagnosticResult(
                                ok = false,
                                summary = Strings.ACCOUNT_CREDENTIALS_UNAVAILABLE,
                                steps = listOf(
                                    WebDavDiagnosticStep(
                                        title = Strings.ACCOUNT_CREDENTIALS_TITLE,
                                        status = WebDavDiagnosticStatus.FAIL,
                                        detail = Strings.ACCOUNT_READD_SERVER,
                                    )
                                ),
                            ),
                        )
                        return@launch
                    }
                    val result = WebDavDiagnosticsService().diagnose(
                        credentials = creds,
                        accountId = active.id,
                        rootPath = active.rootPath,
                        clientFactory = { c, id -> WebDavClient(c, id, app.okHttpClient) },
                    )
                    settingsState = settingsState.copy(
                        isTestingConnection = false,
                        diagnostic = result,
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Throwable) {
                    settingsState = settingsState.copy(
                        isTestingConnection = false,
                        diagnostic = WebDavDiagnosticResult(
                            ok = false,
                            summary = Strings.SETTINGS_WEBDAV_CONNECTION_FAILED,
                            steps = listOf(
                                WebDavDiagnosticStep(
                                    title = Strings.SETTINGS_CONNECTION_TEST,
                                    status = WebDavDiagnosticStatus.FAIL,
                                    detail = AccountErrorMessages.forWebDavError(e),
                                )
                            ),
                        ),
                    )
                }
            }
        },
        onAutoLoadOriginalImagesChange = { enabled ->
            app.preferences.autoLoadOriginalImages = enabled
            settingsState = settingsState.copy(autoLoadOriginalImages = enabled)
        },
        onBlurThumbnailsChange = { enabled ->
            app.preferences.blurThumbnails = enabled
            settingsState = settingsState.copy(blurThumbnails = enabled)
        },
        onGridDensityChange = { density ->
            settingsState = settingsState.copy(gridDensity = density)
            onGridDensityChange(density)
        },
        onClearCache = {
            scope.launch {
                settingsState = settingsState.copy(isClearingCache = true, message = null)
                app.imageLoader.memoryCache?.clear()
                app.imageLoader.diskCache?.clear()
                GeneratedThumbnailCache.clear(app)
                AnimatedThumbnailCache.clear(app)
                settingsState = settingsState.copy(
                    isClearingCache = false,
                    thumbnailCacheBytes = thumbnailCacheBytes(app),
                    message = Strings.SETTINGS_CACHE_CLEARED,
                )
            }
        },
        onClearBookCache = {
            scope.launch {
                settingsState = settingsState.copy(isClearingCache = true, message = null)
                withContext(Dispatchers.IO) { clearAllBookCache(context.cacheDir) }
                settingsState = settingsState.copy(
                    isClearingCache = false,
                    message = Strings.SETTINGS_BOOK_CACHE_CLEARED,
                )
            }
        },
        onCacheLimitChange = { limit ->
            app.preferences.thumbnailCacheLimit = limit
            app.rebuildImageLoader(limit.bytes)
            settingsState = settingsState.copy(
                thumbnailCacheLimit = limit,
                thumbnailCacheBytes = thumbnailCacheBytes(app),
                message = null,
            )
        },
        onCheckUpdate = {
            scope.launch {
                settingsState = settingsState.copy(
                    update = settingsState.update.copy(
                        isChecking = true,
                        errorMessage = null,
                        infoMessage = null,
                    ),
                )
                try {
                    val info = updateService.checkLatest()
                    settingsState = settingsState.copy(
                        update = settingsState.update.copy(
                            isChecking = false,
                            info = info,
                            infoMessage = if (!info.hasUpdate) Strings.SETTINGS_LATEST_VERSION_ALREADY else null,
                        ),
                    )
                } catch (e: Throwable) {
                    settingsState = settingsState.copy(
                        update = settingsState.update.copy(
                            isChecking = false,
                            errorMessage = e.message ?: Strings.SETTINGS_CHECK_UPDATE_FAILED,
                        ),
                    )
                }
            }
        },
        onDownloadUpdate = {
            val info = settingsState.update.info ?: return@SettingsScreen
            scope.launch {
                settingsState = settingsState.copy(
                    update = settingsState.update.copy(
                        isDownloading = true,
                        downloadedBytes = 0L,
                        downloadTotalBytes = info.apkSize,
                        errorMessage = null,
                        infoMessage = null,
                    ),
                )
                try {
                    val downloaded = updateService.downloadApk(info) { received, total ->
                        settingsState = settingsState.copy(
                            update = settingsState.update.copy(
                                downloadedBytes = received,
                                downloadTotalBytes = total ?: settingsState.update.downloadTotalBytes,
                            ),
                        )
                    }
                    settingsState = settingsState.copy(
                        update = settingsState.update.copy(
                            isDownloading = false,
                            downloaded = downloaded,
                            infoMessage = Strings.SETTINGS_UPDATE_SAVED_TO_DOWNLOADS,
                        ),
                    )
                    // Trigger system installer immediately, mirroring Vink Flasher.
                    try {
                        updateService.installApk(downloaded)
                    } catch (_: Throwable) {
                        // Installer launch failure is non-fatal; the APK is on disk.
                    }
                } catch (e: Throwable) {
                    settingsState = settingsState.copy(
                        update = settingsState.update.copy(
                            isDownloading = false,
                            errorMessage = e.message ?: Strings.SETTINGS_DOWNLOAD_UPDATE_FAILED,
                        ),
                    )
                }
            }
        },
        onInstallUpdate = {
            val downloaded = settingsState.update.downloaded ?: return@SettingsScreen
            try {
                updateService.installApk(downloaded)
            } catch (e: Throwable) {
                settingsState = settingsState.copy(
                    update = settingsState.update.copy(
                        errorMessage = e.message ?: Strings.SETTINGS_INSTALLER_FAILED,
                    ),
                )
            }
        },
        onOpenReleasePage = {
            val url = settingsState.update.info?.releaseUrl ?: "https://github.com/Vivitoto/Visto/releases/latest"
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Throwable) { /* no browser */ }
        },
        onDismissUpdateMessage = {
            settingsState = settingsState.copy(
                update = settingsState.update.copy(errorMessage = null, infoMessage = null),
            )
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

private fun thumbnailCacheBytes(app: VistoApplication): Long =
    (app.imageLoader.diskCache?.size ?: 0L) +
        GeneratedThumbnailCache.sizeBytes(app) +
        AnimatedThumbnailCache.sizeBytes(app)

@Composable
private fun BrowserHost(
    summary: AccountSummary,
    credentials: WebDavCredentials,
    repository: RemoteEntryRepository,
    initialPath: String,
    onPathChanged: (String) -> Unit,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as VistoApplication }
    val navigator = remember(summary.id) {
        BrowserNavigator(initialPath = initialPath, rootPath = summary.rootPath)
    }
    var uiState by remember(summary.id) { mutableStateOf(BrowserUiState(currentPath = initialPath)) }
    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var readerSession by remember(summary.id) { mutableStateOf<ReaderSession?>(null) }
    var activeReaderSizeBytes by remember { mutableStateOf<Long?>(null) }
    var activeReaderEtag by remember { mutableStateOf<String?>(null) }
    var activeBrowserLoadJob by remember { mutableStateOf<Job?>(null) }
    var activeReaderJob by remember { mutableStateOf<Job?>(null) }
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
            onPathChanged(currentPath)
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

    fun persistProgress(session: ReaderSession) {
        scope.launch {
            val existing = withContext(Dispatchers.IO) { app.database.bookProgressDao().getByPath(summary.id, session.filePath) }
            saveBookProgress(
                entity = existing,
                session = session,
                accountId = summary.id,
                sizeBytes = activeReaderSizeBytes,
                etag = activeReaderEtag,
                upsert = app.database.bookProgressDao()::upsert,
            )
        }
    }

    fun openBook(book: app.visto.core.model.RemoteEntry) {
        activeReaderJob?.cancel()
        activeReaderSizeBytes = book.sizeBytes
        activeReaderEtag = book.etag
        val defaultReaderSettings = app.preferences.defaultReaderSettings
        readerSession = readerLoadingSession(
            filePath = book.path,
            fileName = book.name,
            defaultSettings = defaultReaderSettings,
        )

        if (book.mediaType == MediaType.EPUB_BOOK) {
            readerSession = readerSession?.copy(
                isLoading = false,
                errorMessage = Strings.READER_EPUB_UNSUPPORTED,
            )
            return
        }

        activeReaderJob = scope.launch {
            try {
                val dao = app.database.bookProgressDao()
                val progress = withContext(Dispatchers.IO) { dao.getByPath(summary.id, book.path) }
                if (progress != null) {
                    readerSession = readerLoadingSession(
                        filePath = book.path,
                        fileName = book.name,
                        progress = progress,
                        defaultSettings = defaultReaderSettings,
                    )
                }
                val result = BookTextLoader.load(client, book.path, context.cacheDir, expectedEtag = book.etag)
                val chapters = ChapterParser.parse(result.text)
                activeReaderSizeBytes = result.sizeBytes
                activeReaderEtag = result.etag
                val loaded = ReaderReducer.reduce(
                    readerSession ?: readerLoadingSession(
                        filePath = book.path,
                        fileName = book.name,
                        progress = progress,
                        defaultSettings = defaultReaderSettings,
                    ),
                    ReaderAction.Loaded(
                        encoding = result.encoding,
                        fullText = result.text,
                        chapters = chapters,
                        currentChapterIndex = progress?.chapterIndex ?: 0,
                        currentPage = progress?.pageOffset ?: 0,
                    ),
                )
                readerSession = loaded
                persistProgress(loaded)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                readerSession = (readerSession ?: readerLoadingSession(
                    filePath = book.path,
                    fileName = book.name,
                    defaultSettings = defaultReaderSettings,
                )).copy(
                    isLoading = false,
                    errorMessage = AccountErrorMessages.forWebDavError(e),
                )
            }
        }
    }

    val activeReader = readerSession
    if (activeReader != null) {
        ActiveReaderScreen(
            session = activeReader,
            onSessionChange = { readerSession = it },
            onClose = {
                activeReaderJob?.cancel()
                activeReaderJob = null
                readerSession = null
            },
            onPersistProgress = ::persistProgress,
            onSetDefaultSettings = { app.preferences.defaultReaderSettings = it.toDefaultSettings() },
        )
        return
    }

    val activeSession = viewerSession
    if (activeSession != null) {
        ViewerScreen(
            session = activeSession,
            imageLoader = app.imageLoader,
            okHttpClient = app.okHttpClient,
            mediaUrlOf = { entry -> client.mediaUrl(entry.path) },
            cacheKeyScope = mediaCacheKeyScope(summary),
            thumbnailCacheLimitBytes = app.preferences.thumbnailCacheLimit.bytes,
            autoLoadOriginalImages = app.preferences.autoLoadOriginalImages,
            onClose = { viewerSession = null },
        )
        return
    }

    BackHandler(enabled = navigator.canGoBack) {
        if (navigator.back() != null) {
            onPathChanged(navigator.currentPath)
            loadCurrent(forceRefresh = false)
        }
    }

    BrowserScreen(
        state = uiState,
        onBack = {
            if (navigator.back() != null) {
                onPathChanged(navigator.currentPath)
                loadCurrent(forceRefresh = false)
            }
        },
        onGoRoot = {
            if (navigator.goRoot() != null) {
                onPathChanged(navigator.currentPath)
                loadCurrent(forceRefresh = false)
            }
        },
        onOpenFolder = { folder ->
            navigator.open(folder.path)
            onPathChanged(navigator.currentPath)
            loadCurrent(forceRefresh = false)
        },
        onOpenMedia = { opened ->
            val all = uiState.media
            viewerSession = ViewerSession.build(all, opened.path)
        },
        onOpenBook = ::openBook,
        onRefresh = { loadCurrent(forceRefresh = true) },
        canGoBack = navigator.canGoBack,
        canGoRoot = navigator.canGoRoot,
        bottomBar = { VistoBottomBar(selected = selectedTab, onSelect = onTabSelected) },
    )
}
