package app.visto.data.album

import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Recursively walks an album root using PROPFIND Depth:1, surfacing
 * incremental [AlbumContents] snapshots so the UI can render progress.
 *
 * v0.1 caps recursion depth and parallelism to keep us friendly to small
 * NAS deployments. The loader is read-only — it never issues anything other
 * than the client's [WebDavClient.listDirectory] call.
 */
class AlbumLoader(
    private val client: WebDavClient,
    private val maxDepth: Int = 8,
    private val maxParallel: Int = 4,
) {

    /**
     * Stream snapshots of the album contents as folders are visited.
     *
     * Emissions:
     *  - initial empty snapshot
     *  - one snapshot after each visited folder
     *  - final snapshot when traversal is complete
     */
    fun load(rootPath: String): Flow<AlbumContents> = flow {
        val normalizedRoot = DavPath.normalize(rootPath)
        emit(AlbumContents(normalizedRoot, emptyList(), 0, 0, 0, emptyList()))

        val collected = mutableListOf<RemoteEntry>()
        val warnings = mutableListOf<String>()
        var visited = 0
        var failed = 0

        // BFS one level at a time so PROPFIND failures on one branch never
        // block sibling branches.
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
                val outcome = result.outcome
                when (outcome) {
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
                            } else {
                                collected += entry
                            }
                        }
                    }
                }
                val sections = AlbumGrouper.group(normalizedRoot, collected)
                emit(
                    AlbumContents(
                        rootPath = normalizedRoot,
                        sections = sections,
                        totalMedia = sections.sumOf { it.media.size },
                        foldersVisited = visited,
                        foldersFailed = failed,
                        warnings = warnings.toList(),
                    )
                )
            }
            frontier = nextFrontier
        }
    }

    private suspend fun fetch(path: String): FetchOutcome = try {
        FetchOutcome.Success(client.listDirectory(path))
    } catch (ce: kotlinx.coroutines.CancellationException) {
        // Rethrow so the outer scope can collapse cleanly when the user
        // navigates back; we must never swallow cancellation and report it
        // as a per-folder failure.
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

/**
 * Helper to await the *final* contents (last emission). Useful in tests and
 * places that don't want streaming.
 */
suspend fun AlbumLoader.loadCollected(rootPath: String): AlbumContents {
    var last: AlbumContents? = null
    load(rootPath).collect { last = it }
    return last ?: AlbumContents(rootPath, emptyList(), 0, 0, 0, emptyList())
}
