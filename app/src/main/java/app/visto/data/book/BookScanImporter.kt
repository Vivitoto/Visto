package app.visto.data.book

import app.visto.core.model.RemoteEntry
import app.visto.data.account.ReaderDefaultSettings
import app.visto.data.db.BookProgressDao
import app.visto.data.db.BookProgressEntity

data class BookScanImportResult(
    val imported: Int,
    val updated: Int,
)

object BookScanImporter {

    fun importBooks(
        accountId: Long,
        books: List<RemoteEntry>,
        now: Long,
        defaultSettings: ReaderDefaultSettings,
        dao: BookProgressDao,
    ): BookScanImportResult {
        var imported = 0
        var updated = 0
        for (book in books) {
            val existing = dao.getByPath(accountId, book.path)
            if (existing == null) {
                dao.upsert(scannedBookProgress(accountId, book, now, defaultSettings))
                imported += 1
            } else {
                dao.updateBookMetadata(
                    accountId = accountId,
                    path = book.path,
                    name = book.name,
                    sizeBytes = book.sizeBytes,
                    etag = book.etag,
                )
                updated += 1
            }
        }
        return BookScanImportResult(imported = imported, updated = updated)
    }
}

internal fun scannedBookProgress(
    accountId: Long,
    entry: RemoteEntry,
    now: Long,
    defaultSettings: ReaderDefaultSettings,
): BookProgressEntity {
    val margins = defaultSettings.pageMargins.clamped()
    return BookProgressEntity(
        accountId = accountId,
        path = entry.path,
        name = entry.name,
        sizeBytes = entry.sizeBytes,
        etag = entry.etag,
        encoding = Charsets.UTF_8.name(),
        chapterIndex = 0,
        chapterTitle = null,
        pageOffset = 0,
        pageStartChar = null,
        totalChapters = 0,
        fontSizeSp = defaultSettings.fontSizeSp,
        lineSpacing = defaultSettings.lineSpacing,
        theme = defaultSettings.theme,
        fontChoice = defaultSettings.fontChoice,
        textColor = defaultSettings.textColor,
        backgroundStyle = defaultSettings.backgroundStyle,
        pageMarginTopDp = margins.topDp,
        pageMarginBottomDp = margins.bottomDp,
        pageMarginStartDp = margins.startDp,
        pageMarginEndDp = margins.endDp,
        lastReadAt = now,
        addedAt = now,
    )
}
