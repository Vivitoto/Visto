package app.visto.ui

/**
 * Centralized Chinese UI strings for Visto v0.2.
 *
 * Kept as a Kotlin object (rather than `strings.xml`) to keep build and
 * preview cycles fast; we can lift these into resources once we localize
 * beyond zh-CN.
 */
object Strings {
    // App-wide
    const val APP_NAME = "Visto"
    const val LOADING = "加载中…"

    // Account screen
    const val ACCOUNT_TITLE = "连接 WebDAV 账号"
    const val ACCOUNT_DISPLAY_NAME = "显示名称"
    const val ACCOUNT_SERVER_URL = "服务器地址 (https://...)"
    const val ACCOUNT_USERNAME = "用户名"
    const val ACCOUNT_PASSWORD = "密码 / 应用令牌"
    const val ACCOUNT_ROOT_PATH = "根路径"
    const val ACCOUNT_TEST_CONNECTION = "测试连接"
    const val ACCOUNT_SAVE_AND_USE = "保存并使用"
    const val ACCOUNT_TESTING = "测试中…"
    const val ACCOUNT_SAVING = "保存中…"
    const val ACCOUNT_CONNECTION_OK = "连接成功"

    // Account errors
    const val ERR_INVALID_URL = "服务器地址必须以 http:// 或 https:// 开头"
    const val ERR_EMPTY_USERNAME = "用户名不能为空"
    const val ERR_EMPTY_PASSWORD = "密码不能为空"
    const val ERR_AUTH = "认证失败，请检查账号和密码"
    const val ERR_NOT_FOUND = "服务器找不到该路径"
    const val ERR_TIMEOUT = "连接超时，请检查网络"
    const val ERR_NETWORK = "网络错误，请稍后重试"
    const val ERR_SERVER = "服务器错误，请稍后重试"
    const val ERR_UNEXPECTED = "未知错误"

    // Browser (legacy directory mode)
    const val BROWSER_FOLDERS = "文件夹"
    const val BROWSER_MEDIA = "媒体"
    const val BROWSER_EMPTY = "此目录为空"
    const val BROWSER_LOAD_FAILED = "加载失败"

    // Albums
    const val ALBUMS_TITLE = "相册"
    const val ALBUMS_EMPTY_TITLE = "还没有相册"
    const val ALBUMS_EMPTY_SUBTITLE = "添加一个 WebDAV 路径，开始浏览"
    const val ALBUMS_ADD = "添加相册"
    const val ALBUMS_ADD_DIALOG_TITLE = "添加相册"
    const val ALBUMS_NAME_LABEL = "相册名称"
    const val ALBUMS_PATH_LABEL = "WebDAV 路径（例如 /Photos/Family）"
    const val ALBUMS_PATH_HINT = "应用会递归读取该路径下的所有媒体"
    const val ALBUMS_SAVE = "保存"
    const val ALBUMS_CANCEL = "取消"
    const val ALBUMS_DELETE = "删除"
    const val ALBUMS_DELETE_CONFIRM_TITLE = "删除相册"
    const val ALBUMS_DELETE_CONFIRM_MESSAGE = "只会从相册列表移除，不会删除 WebDAV 上的文件。"
    const val ALBUMS_ERR_PATH_REQUIRED = "请输入 WebDAV 路径"
    const val ALBUMS_ERR_PATH_MUST_START_WITH_SLASH = "路径必须以 / 开头"
    const val ALBUMS_ERR_DUPLICATE = "已经添加过这个路径了"

    // Album detail
    const val ALBUM_DETAIL_ROOT_SECTION = "根目录"
    const val ALBUM_DETAIL_REFRESH = "刷新"
    const val ALBUM_DETAIL_LOADING = "正在读取 WebDAV…"
    const val ALBUM_DETAIL_NO_MEDIA = "未找到媒体文件"

    fun albumDetailProgress(visited: Int, failed: Int): String {
        val base = "已扫描 $visited 个目录"
        return if (failed == 0) base else "$base，$failed 个失败"
    }

    fun albumDetailSectionCount(count: Int): String = "$count 个文件"

    // Settings
    const val SETTINGS_TITLE = "设置"
    const val SETTINGS_ACCOUNT = "账号"
    const val SETTINGS_ROOT_LABEL = "根路径："
    const val SETTINGS_BROWSE_MODE = "浏览方式"
    const val SETTINGS_BROWSE_MODE_ALBUMS = "相册（默认）"
    const val SETTINGS_BROWSE_MODE_DIR = "按目录浏览（旧版）"
    const val SETTINGS_LOCAL_CACHE = "本地缓存"
    const val SETTINGS_CLEAR_THUMBNAILS = "清除本地缩略图"
    const val SETTINGS_CLEARING = "清理中…"
    const val SETTINGS_CACHE_CLEARED = "本地缩略图已清除，未触碰 WebDAV 上的文件。"

    fun thumbnailsOnDisk(formatted: String): String = "本地缩略图占用：$formatted"
}
