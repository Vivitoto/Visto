package app.visto.data.webdav

import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import kotlinx.coroutines.CancellationException

/**
 * Read-only WebDAV connection diagnostics shared by account setup and settings.
 *
 * It deliberately reuses [WebDavClient.listDirectory] so the test mirrors the
 * real browsing path and never performs write operations.
 */
class WebDavDiagnosticsService {

    suspend fun diagnose(
        credentials: WebDavCredentials,
        accountId: Long,
        rootPath: String,
        clientFactory: (WebDavCredentials, Long) -> WebDavClient = { creds, id ->
            WebDavClient(credentials = creds, accountId = id)
        },
    ): WebDavDiagnosticResult {
        val steps = mutableListOf<WebDavDiagnosticStep>()
        val normalizedRoot = try {
            if (DavPath.hasDotSegments(rootPath)) throw WebDavError.InvalidPath()
            DavPath.normalize(rootPath)
        } catch (error: Throwable) {
            steps += fail("路径格式", "路径不能包含 . 或 .. 段")
            return WebDavDiagnosticResult(false, "WebDAV 路径不可用", steps)
        }

        if (!credentials.baseUrl.trim().startsWith("http://") && !credentials.baseUrl.trim().startsWith("https://")) {
            steps += fail("服务器地址", "服务器地址必须以 http:// 或 https:// 开头")
            return WebDavDiagnosticResult(false, "服务器地址不可用", steps)
        }
        steps += pass("服务器地址", "格式正确：${credentials.baseUrl.trim()}")

        val entries = try {
            clientFactory(credentials, accountId).listDirectory(normalizedRoot)
        } catch (ce: CancellationException) {
            throw ce
        } catch (error: Throwable) {
            steps += fail("认证与目录读取", messageFor(error))
            return WebDavDiagnosticResult(false, "WebDAV 连接失败", steps)
        }

        steps += pass("认证与目录读取", "账号可用，根路径可读取：$normalizedRoot")
        val folderCount = entries.count { it.isDirectory }
        val imageCount = entries.count { !it.isDirectory && (it.mediaType == MediaType.IMAGE || it.mediaType == MediaType.ANIMATED_IMAGE) }
        val videoCount = entries.count { !it.isDirectory && it.mediaType == MediaType.VIDEO }
        val otherCount = entries.size - folderCount - imageCount - videoCount
        steps += pass(
            "媒体识别",
            "${folderCount} 个文件夹，${imageCount} 张图片，${videoCount} 个视频，${otherCount.coerceAtLeast(0)} 个其他文件",
        )
        return WebDavDiagnosticResult(true, "WebDAV 连接正常", steps)
    }

    private fun pass(title: String, detail: String): WebDavDiagnosticStep =
        WebDavDiagnosticStep(title, WebDavDiagnosticStatus.PASS, detail)

    private fun fail(title: String, detail: String): WebDavDiagnosticStep =
        WebDavDiagnosticStep(title, WebDavDiagnosticStatus.FAIL, detail)

    private fun messageFor(error: Throwable): String = when (error) {
        is WebDavError.AuthFailed -> "认证失败，请检查用户名或密码。"
        is WebDavError.NotFound -> "根路径不存在，请确认 WebDAV 路径。"
        is WebDavError.Timeout -> "连接超时，请检查网络或代理。"
        is WebDavError.NetworkError -> "网络连接失败，请检查服务器地址、网络或代理。"
        is WebDavError.ServerError -> "服务器返回错误：HTTP ${error.statusCode}。"
        is WebDavError.ParseError -> "服务器响应不是有效的 WebDAV 目录列表。"
        is WebDavError.InvalidPath -> "路径不能包含 . 或 .. 段。"
        is WebDavError.Unexpected -> "服务器返回异常状态：HTTP ${error.statusCode}。"
        else -> "${error.message ?: error.javaClass.simpleName}"
    }
}
