package app.visto.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.visto.ui.reader.ReaderPageMargins

internal object BookProgressDefaults {
    const val DEFAULT_PAGE_MARGIN_TOP_DP = ReaderPageMargins.DEFAULT_TOP_DP
    const val DEFAULT_PAGE_MARGIN_BOTTOM_DP = ReaderPageMargins.DEFAULT_BOTTOM_DP
    const val DEFAULT_PAGE_MARGIN_HORIZONTAL_DP = ReaderPageMargins.DEFAULT_HORIZONTAL_DP
}

/**
 * Saved reading position and per-book reader preferences for one WebDAV file.
 */
@Entity(
    tableName = "book_progress",
    foreignKeys = [
        ForeignKey(
            entity = DavAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "path"], unique = true)],
)
data class BookProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "sizeBytes")
    val sizeBytes: Long?,
    @ColumnInfo(name = "etag")
    val etag: String?,
    @ColumnInfo(name = "encoding")
    val encoding: String,
    @ColumnInfo(name = "chapterIndex")
    val chapterIndex: Int = 0,
    @ColumnInfo(name = "chapterTitle")
    val chapterTitle: String?,
    @ColumnInfo(name = "pageOffset")
    val pageOffset: Int = 0,
    @ColumnInfo(name = "totalChapters")
    val totalChapters: Int = 0,
    @ColumnInfo(name = "fontSizeSp")
    val fontSizeSp: Int = 18,
    @ColumnInfo(name = "lineSpacing")
    val lineSpacing: Float = 1.5f,
    @ColumnInfo(name = "theme")
    val theme: String = "light",
    @ColumnInfo(name = "fontChoice")
    val fontChoice: String = "system",
    @ColumnInfo(name = "textColor")
    val textColor: String = "default",
    @ColumnInfo(name = "backgroundStyle")
    val backgroundStyle: String = "default",
    @ColumnInfo(name = "pageMarginTopDp")
    val pageMarginTopDp: Int = BookProgressDefaults.DEFAULT_PAGE_MARGIN_TOP_DP,
    @ColumnInfo(name = "pageMarginBottomDp")
    val pageMarginBottomDp: Int = BookProgressDefaults.DEFAULT_PAGE_MARGIN_BOTTOM_DP,
    @ColumnInfo(name = "pageMarginStartDp")
    val pageMarginStartDp: Int = BookProgressDefaults.DEFAULT_PAGE_MARGIN_HORIZONTAL_DP,
    @ColumnInfo(name = "pageMarginEndDp")
    val pageMarginEndDp: Int = BookProgressDefaults.DEFAULT_PAGE_MARGIN_HORIZONTAL_DP,
    @ColumnInfo(name = "lastReadAt")
    val lastReadAt: Long,
    @ColumnInfo(name = "addedAt")
    val addedAt: Long,
)
