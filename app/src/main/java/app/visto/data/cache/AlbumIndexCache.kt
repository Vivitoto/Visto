package app.visto.data.cache

import android.content.Context
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Local JSON cache for WebDAV directory listings.
 *
 * Each account + [path] pair is mapped to a file under `cacheDir/album_index/`.
 * When the user re-opens a previously-visited directory the cached entries are
 * displayed immediately while a background network refresh runs.
 */
object AlbumIndexCache {

    private const val CACHE_DIR = "album_index"

    private fun cacheFile(context: Context, accountId: Long, path: String): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${cacheKey(accountId, path)}.json")
    }

    fun load(context: Context, accountId: Long, path: String): List<RemoteEntry>? {
        val file = cacheFile(context, accountId, path)
        if (!file.exists()) return null
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            null
        }
    }

    fun save(context: Context, accountId: Long, path: String, entries: List<RemoteEntry>) {
        val file = cacheFile(context, accountId, path)
        try {
            val arr = JSONArray()
            entries.forEach { arr.put(toJson(it)) }
            file.writeText(arr.toString())
        } catch (_: Exception) {
            // Non-critical; silently skip cache write on I/O error.
        }
    }

    fun clear(context: Context) {
        val dir = File(context.cacheDir, CACHE_DIR)
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun cacheKey(accountId: Long, path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(path.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "account_${accountId}_$digest"
    }

    // -- serialisation -------------------------------------------------------

    private fun toJson(e: RemoteEntry): JSONObject = JSONObject().apply {
        put("accountId", e.accountId)
        put("parentPath", if (e.parentPath == null) JSONObject.NULL else e.parentPath)
        put("path", e.path)
        put("name", e.name)
        put("isDirectory", e.isDirectory)
        put("mediaType", e.mediaType.name)
        put("mimeType", if (e.mimeType == null) JSONObject.NULL else e.mimeType)
        put("sizeBytes", if (e.sizeBytes == null) JSONObject.NULL else e.sizeBytes)
        put("etag", if (e.etag == null) JSONObject.NULL else e.etag)
        put("lastModifiedEpochMs", if (e.lastModifiedEpochMs == null) JSONObject.NULL else e.lastModifiedEpochMs)
    }

    private fun fromJson(obj: JSONObject): RemoteEntry = RemoteEntry(
        accountId = obj.getLong("accountId"),
        parentPath = if (obj.isNull("parentPath")) null else obj.getString("parentPath"),
        path = obj.getString("path"),
        name = obj.getString("name"),
        isDirectory = obj.getBoolean("isDirectory"),
        mediaType = MediaType.valueOf(obj.getString("mediaType")),
        mimeType = if (obj.isNull("mimeType")) null else obj.getString("mimeType"),
        sizeBytes = if (obj.isNull("sizeBytes")) null else obj.getLong("sizeBytes"),
        etag = if (obj.isNull("etag")) null else obj.getString("etag"),
        lastModifiedEpochMs = if (obj.isNull("lastModifiedEpochMs")) null else obj.getLong("lastModifiedEpochMs"),
    )
}
