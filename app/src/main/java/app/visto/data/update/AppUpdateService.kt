package app.visto.data.update

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.visto.AppInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

/**
 * In-app update checker, downloader and installer for Visto.
 *
 * App update service layout:
 *   - Source of truth is the GitHub Release tagged `latest` on
 *     `Vivitoto/Visto`. Visto does not maintain an R2 mirror.
 *   - APKs are downloaded into the public Downloads folder (MediaStore on
 *     Android Q+, legacy `Environment.DIRECTORY_DOWNLOADS` on older).
 *   - Install is triggered with `Intent.ACTION_VIEW` against a
 *     `application/vnd.android.package-archive` URI; the system installer
 *     prompts the user (and asks for "allow from this source" the first
 *     time when needed).
 */
class AppUpdateService(
    private val context: Context,
    private val client: OkHttpClient,
) {
    /** Query GitHub's REST API for the latest release. */
    suspend fun checkLatest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Visto-Updater")
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (error: IOException) {
            throw IOException(githubNetworkMessage(error), error)
        }
        response.use { response ->
            if (!response.isSuccessful) {
                throw IOException(githubHttpMessage(response.code, response.body?.string().orEmpty()))
            }
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isEmpty()) {
                throw IOException("GitHub 返回空响应，可能是网络或代理异常。")
            }
            val json = try {
                JSONObject(body)
            } catch (error: JSONException) {
                throw IOException("GitHub 返回异常内容，可能是代理或网络劫持。", error)
            }
            val assets = json.optJSONArray("assets") ?: throw IOException("latest 发布版本中没有资产。")
            var apkName = ""
            var apkUrl = ""
            var apkSize: Long? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.lowercase().endsWith(".apk") && name.lowercase().startsWith("visto-")) {
                    apkName = name
                    apkUrl = asset.optString("browser_download_url")
                    apkSize = asset.optLong("size").takeIf { it > 0 }
                    break
                }
            }
            if (apkName.isEmpty() || apkUrl.isEmpty()) {
                throw IOException("latest 发布版本中没有找到 Visto APK 安装包。")
            }
            val latestVersion = versionFromApkName(apkName) ?: AppInfo.VERSION_NAME
            AppUpdateInfo(
                currentVersion = AppInfo.VERSION_NAME,
                currentVersionCode = AppInfo.VERSION_CODE,
                latestVersion = latestVersion,
                hasUpdate = compareVersions(latestVersion, AppInfo.VERSION_NAME) > 0,
                apkName = apkName,
                apkUrl = apkUrl,
                releaseUrl = json.optString("html_url"),
                releaseNotes = json.optString("body").trim(),
                apkSize = apkSize,
            )
        }
    }

    /**
     * Download [update]'s APK into the public Downloads folder.
     *
     * Reports progress through [onProgress]. The returned [DownloadedApk]
     * carries the content URI / file path that can be handed to
     * [installApk]. Existing files with the same name are overwritten.
     */
    suspend fun downloadApk(
        update: AppUpdateInfo,
        onProgress: (received: Long, total: Long?) -> Unit,
    ): DownloadedApk = withContext(Dispatchers.IO) {
        val safeName = update.apkName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val connection = try {
            (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Visto-Updater")
            }
        } catch (error: IOException) {
            throw IOException(githubNetworkMessage(error), error)
        }
        val expectedSize = try {
            update.apkSize ?: connection.contentLengthLong.takeIf { it > 0 }
        } catch (error: IOException) {
            connection.disconnect()
            throw IOException(githubNetworkMessage(error), error)
        }
        try {
            if (connection.responseCode !in 200..299) {
                val code = connection.responseCode
                throw IOException("APK 下载失败：HTTP $code。请检查 GitHub 访问或稍后重试。")
            }
        } catch (error: IOException) {
            connection.disconnect()
            if (error.message?.startsWith("APK 下载失败") == true) throw error
            throw IOException(githubNetworkMessage(error), error)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeApkToMediaStore(connection, safeName, expectedSize, onProgress)
            } else {
                writeApkToLegacyDownloads(connection, safeName, expectedSize, onProgress)
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Launch the system package installer for the downloaded [apk]. */
    fun installApk(apk: DownloadedApk) {
        val uri = Uri.parse(apk.uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun writeApkToMediaStore(
        connection: HttpURLConnection,
        name: String,
        total: Long?,
        onProgress: (Long, Long?) -> Unit,
    ): DownloadedApk {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf(name),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                resolver.delete(Uri.withAppendedPath(collection, id.toString()), null, null)
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("\u65e0\u6cd5\u5728\u7cfb\u7edf\u4e0b\u8f7d\u76ee\u5f55\u521b\u5efa\u6587\u4ef6")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                streamHttpToOutput(connection, output, total, onProgress)
            } ?: throw IOException("\u65e0\u6cd5\u6253\u5f00\u4e0b\u8f7d\u76ee\u5f55\u8f93\u51fa\u6d41")
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            return DownloadedApk(name = name, uri = uri.toString(), path = null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun writeApkToLegacyDownloads(
        connection: HttpURLConnection,
        name: String,
        total: Long?,
        onProgress: (Long, Long?) -> Unit,
    ): DownloadedApk {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        if (file.exists()) file.delete()
        FileOutputStream(file).use { output ->
            streamHttpToOutput(connection, output, total, onProgress)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return DownloadedApk(name = name, uri = uri.toString(), path = file.absolutePath)
    }

    private fun streamHttpToOutput(
        connection: HttpURLConnection,
        output: java.io.OutputStream,
        total: Long?,
        onProgress: (Long, Long?) -> Unit,
    ) {
        val buffer = ByteArray(128 * 1024)
        var received = 0L
        connection.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                received += read.toLong()
                onProgress(received, total)
            }
        }
        output.flush()
        if (total != null && total > 0 && received != total) {
            throw IOException("APK 下载不完整：期望 ${formatBytes(total)}，实际 ${formatBytes(received)}。请重新下载。")
        }
    }

    private fun versionFromApkName(name: String): String? {
        val match = Regex("visto-v([0-9_]+)\\.apk", RegexOption.IGNORE_CASE).find(name) ?: return null
        return match.groupValues[1].replace('_', '.')
    }

    private fun compareVersions(a: String, b: String): Int {
        val left = a.split('.').map { it.toIntOrNull() ?: 0 }
        val right = b.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(left.size, right.size)
        for (i in 0 until n) {
            val l = if (i < left.size) left[i] else 0
            val r = if (i < right.size) right[i] else 0
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun githubHttpMessage(code: Int, body: String): String = when (code) {
        403 -> if (body.contains("rate limit", ignoreCase = true)) {
            "GitHub API 请求次数受限，请稍后再试。"
        } else {
            "GitHub 拒绝了更新检查请求（HTTP 403），请检查网络或代理。"
        }
        404 -> "未找到 Visto 的 latest 发布版本。"
        in 500..599 -> "GitHub 服务暂时不可用（HTTP $code），请稍后重试。"
        else -> "检查更新失败：GitHub 返回 HTTP $code。"
    }

    private fun githubNetworkMessage(error: IOException): String = when (error) {
        is SocketTimeoutException -> "连接 GitHub 超时，请检查网络或代理。"
        is UnknownHostException -> "无法解析 GitHub 地址，请检查 DNS、网络或代理。"
        else -> "连接 GitHub 失败：${error.message ?: error.javaClass.simpleName}"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return "%.1f %s".format(value, units[unit])
    }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/Vivitoto/Visto/releases/tags/latest"
    }
}
