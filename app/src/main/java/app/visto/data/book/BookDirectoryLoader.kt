package app.visto.data.book

import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class BookDirectoryScanResult(
    val rootPath: String,
    val books: List<RemoteEntry>,
    val foldersVisited: Int,
    val foldersFailed: Int,
    val warnings: List<String>,
)

object BookDirectoryScanner {
    private val BOOK_TYPES = setOf(MediaType.TEXT_BOOK, MediaType.EPUB_BOOK)

    fun isBookEntry(entry: RemoteEntry): Boolean =
        !entry.isDirectory && entry.mediaType in BOOK_TYPES
}

/**
 * Recursively walks a WebDAV directory and collects book files. The traversal
 * uses Depth:1 listings, bounded depth, and modest parallelism like albums.
 */
class BookDirectoryLoader(
    private val client: WebDavClient,
    private val maxDepth: Int = 8,
    private val maxParallel: Int = 4,
) {
    suspend fun loadCollected(rootPath: String): BookDirectoryScanResult {
        val normalizedRoot = DavPath.normalize(rootPath)
        val collected = linkedMapOf<String, RemoteEntry>()
        val warnings = mutableListOf<String>()
        var visited = 0
        var failed = 0

        var frontier = listOf(LevelEntry(normalizedRoot, depth = 0))
        val semaphore = Semaphore(maxParallel.coerceAtLeast(1))

        while (frontier.isNotEmpty()) {
            val results = coroutineScope {
                frontier.map { node ->
                    async {
                        semaphore.withPermit { fetch(node.path) }
                            .let { LevelResult(node, it) }
                    }
                }.awaitAll()
            }

            val nextFrontier = mutableListOf<LevelEntry>()
            for (result in results) {
                visited += 1
                when (val outcome = result.outcome) {
                    is FetchOutcome.Failure -> {
                        failed += 1
                        warnings += "无法访问 ${result.node.path}：${outcome.message}"
                    }
                    is FetchOutcome.Success -> {
                        for (entry in outcome.entries) {
                            if (entry.isDirectory) {
                                if (result.node.depth + 1 <= maxDepth) {
                                    nextFrontier += LevelEntry(entry.path, result.node.depth + 1)
                                }
                            } else if (BookDirectoryScanner.isBookEntry(entry)) {
                                collected[entry.path] = entry
                            }
                        }
                    }
                }
            }
            frontier = nextFrontier
        }

        return BookDirectoryScanResult(
            rootPath = normalizedRoot,
            books = collected.values.toList(),
            foldersVisited = visited,
            foldersFailed = failed,
            warnings = warnings.toList(),
        )
    }

    private suspend fun fetch(path: String): FetchOutcome = try {
        FetchOutcome.Success(client.listDirectory(path))
    } catch (ce: kotlinx.coroutines.CancellationException) {
        throw ce
    } catch (t: Throwable) {
        FetchOutcome.Failure(t.message ?: t::class.simpleName ?: "未知错误")
    }

    private data class LevelEntry(val path: String, val depth: Int)
    private data class LevelResult(val node: LevelEntry, val outcome: FetchOutcome)
    private sealed interface FetchOutcome {
        data class Success(val entries: List<RemoteEntry>) : FetchOutcome
        data class Failure(val message: String) : FetchOutcome
    }
}
