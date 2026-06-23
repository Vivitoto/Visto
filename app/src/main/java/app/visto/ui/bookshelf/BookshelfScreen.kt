package app.visto.ui.bookshelf

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.data.db.BookProgressEntity
import app.visto.ui.HomeTab
import app.visto.ui.Strings
import app.visto.ui.VistoBottomBar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    state: BookshelfUiState,
    onOpenBook: (BookProgressEntity) -> Unit,
    onRemoveBook: (BookProgressEntity) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
) {
    var selectedBook by remember { mutableStateOf<BookProgressEntity?>(null) }
    var bookPendingRemove by remember { mutableStateOf<BookProgressEntity?>(null) }
    var layoutMode by remember { mutableStateOf(BookshelfLayoutMode.GRID_3) }

    Scaffold(
        bottomBar = { VistoBottomBar(selected = HomeTab.BOOKSHELF, onSelect = onTabSelected) },
    ) { innerPadding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> BookshelfSkeleton()
                state.books.isEmpty() -> BookshelfEmptyState()
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    BookshelfViewModeBar(
                        current = layoutMode,
                        onChange = { layoutMode = it },
                    )
                    when (layoutMode) {
                        BookshelfLayoutMode.LIST -> BookshelfList(
                            books = state.books,
                            onOpenBook = onOpenBook,
                            onSelectBook = { selectedBook = it },
                        )
                        BookshelfLayoutMode.GRID_3,
                        BookshelfLayoutMode.GRID_5 -> BookshelfGrid(
                            books = state.books,
                            columns = layoutMode.gridColumns ?: 3,
                            onOpenBook = onOpenBook,
                            onSelectBook = { selectedBook = it },
                        )
                    }
                }
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    val actionBook = selectedBook
    if (actionBook != null) {
        ModalBottomSheet(onDismissRequest = { selectedBook = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = BookshelfStateBuilder.displayTitle(actionBook),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = actionBook.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = {
                        selectedBook = null
                        onOpenBook(actionBook)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.BOOKSHELF_CONTINUE_READING) }
                OutlinedButton(
                    onClick = {
                        selectedBook = null
                        bookPendingRemove = actionBook
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = Strings.BOOKSHELF_REMOVE,
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(
                    onClick = { selectedBook = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(Strings.ALBUMS_CANCEL) }
            }
        }
    }

    val removeBook = bookPendingRemove
    if (removeBook != null) {
        AlertDialog(
            onDismissRequest = { bookPendingRemove = null },
            title = { Text(Strings.BOOKSHELF_REMOVE) },
            text = { Text(Strings.BOOKSHELF_REMOVE_CONFIRM_MESSAGE) },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookPendingRemove = null
                        onRemoveBook(removeBook)
                    },
                ) { Text(Strings.BOOKSHELF_REMOVE_CONFIRM, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingRemove = null }) { Text(Strings.ALBUMS_CANCEL) }
            },
        )
    }
}

@Composable
private fun BookshelfViewModeBar(
    current: BookshelfLayoutMode,
    onChange: (BookshelfLayoutMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Strings.BOOKSHELF_VIEW_MODE,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        BookshelfViewCycleButton(
            current = current,
            onClick = { onChange(current.next()) },
        )
    }
}

@Composable
private fun BookshelfViewCycleButton(
    current: BookshelfLayoutMode,
    onClick: () -> Unit,
) {
    val next = current.next()
    val isList = current == BookshelfLayoutMode.LIST
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isList) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (next == BookshelfLayoutMode.LIST) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
            tint = if (isList) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = next.gridColumns
                ?.let(Strings::bookshelfSwitchToGrid)
                ?: Strings.BOOKSHELF_SWITCH_TO_LIST,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun BookshelfGrid(
    books: List<BookProgressEntity>,
    columns: Int,
    onOpenBook: (BookProgressEntity) -> Unit,
    onSelectBook: (BookProgressEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(books, key = BookshelfStateBuilder::stableBookKey) { book ->
            BookshelfBookCard(
                book = book,
                onClick = { onOpenBook(book) },
                onLongClick = { onSelectBook(book) },
            )
        }
    }
}

@Composable
private fun BookshelfList(
    books: List<BookProgressEntity>,
    onOpenBook: (BookProgressEntity) -> Unit,
    onSelectBook: (BookProgressEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listItems(books, key = BookshelfStateBuilder::stableBookKey) { book ->
            BookshelfListRow(
                book = book,
                onClick = { onOpenBook(book) },
                onLongClick = { onSelectBook(book) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfBookCard(
    book: BookProgressEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookshelfBookCover(book = book)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = BookshelfStateBuilder.progressSummary(book),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { BookshelfStateBuilder.readingProgressFraction(book) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            )
            Text(
                text = Strings.bookshelfLastRead(BookshelfStateBuilder.relativeLastReadTime(book.lastReadAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfListRow(
    book: BookProgressEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookshelfBookCover(
            book = book,
            modifier = Modifier.width(70.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = BookshelfStateBuilder.displayTitle(book),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = BookshelfStateBuilder.progressSummary(book),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { BookshelfStateBuilder.readingProgressFraction(book) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            )
            Text(
                text = Strings.bookshelfLastRead(BookshelfStateBuilder.relativeLastReadTime(book.lastReadAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BookshelfBookCover(
    book: BookProgressEntity,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val cover = BookshelfStateBuilder.coverPresentation(book)
    when (cover.source) {
        BookshelfCoverSource.GENERATED_PLACEHOLDER,
        BookshelfCoverSource.EPUB_EMBEDDED -> GeneratedBookCover(
            book = book,
            cover = cover,
            modifier = modifier,
        )
    }
}

@Composable
private fun GeneratedBookCover(
    book: BookProgressEntity,
    cover: BookshelfCoverPresentation,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val palette = BookCoverPalettes[BookshelfStateBuilder.coverPaletteIndex(book, BookCoverPalettes.size)]
    val coverTitle = BookshelfStateBuilder.coverTitlePresentation(book)
    val progressLabel = BookshelfStateBuilder.readingProgressPercentLabel(book)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(colors = listOf(palette.top, palette.bottom))),
    ) {
        val compact = maxWidth < 96.dp
        val spineWidth = if (compact) 5.dp else 8.dp
        val horizontalPadding = if (compact) 10.dp else 30.dp
        val trailingPadding = if (compact) 10.dp else 18.dp
        val topPadding = if (compact) 10.dp else 24.dp
        val bottomPadding = if (compact) 10.dp else 16.dp
        val titleTopPadding = if (compact) 28.dp else 48.dp
        val titleBottomPadding = if (compact) 30.dp else 48.dp
        val titleMaxLines = if (coverTitle.subtitle == null) {
            if (compact) 3 else 5
        } else {
            if (compact) 2 else 4
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(spineWidth)
                .background(Color.Black.copy(alpha = 0.18f)),
        )
        if (!compact) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = horizontalPadding, top = topPadding)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.accent.copy(alpha = 0.92f)),
            )
        }
        Text(
            text = cover.fileType.coverBadge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (compact) 8.dp else 14.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.ink.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    top = titleTopPadding,
                    end = trailingPadding,
                    bottom = titleBottomPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        ) {
            Text(
                text = coverTitle.title,
                modifier = Modifier.fillMaxWidth(),
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                textAlign = TextAlign.Center,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            coverTitle.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = palette.ink.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = horizontalPadding, end = trailingPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!compact) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(palette.ink.copy(alpha = 0.36f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(palette.ink.copy(alpha = 0.26f)),
                )
            }
            progressLabel?.let {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class BookCoverPalette(
    val top: Color,
    val bottom: Color,
    val accent: Color,
    val ink: Color,
)

private val BookCoverPalettes = listOf(
    BookCoverPalette(
        top = Color(0xFF8D3D43),
        bottom = Color(0xFF3B1E2A),
        accent = Color(0xFFE7B866),
        ink = Color(0xFFFFF7E8),
    ),
    BookCoverPalette(
        top = Color(0xFF2F746F),
        bottom = Color(0xFF123B44),
        accent = Color(0xFFE9CF8C),
        ink = Color(0xFFF4FAF4),
    ),
    BookCoverPalette(
        top = Color(0xFF715C2F),
        bottom = Color(0xFF30351E),
        accent = Color(0xFFD8D07A),
        ink = Color(0xFFFFF9DD),
    ),
    BookCoverPalette(
        top = Color(0xFF4E5E91),
        bottom = Color(0xFF232941),
        accent = Color(0xFFFFC67A),
        ink = Color(0xFFF4F6FF),
    ),
    BookCoverPalette(
        top = Color(0xFF7A4C6B),
        bottom = Color(0xFF352338),
        accent = Color(0xFFF0B98D),
        ink = Color(0xFFFFF4F8),
    ),
    BookCoverPalette(
        top = Color(0xFF8A5234),
        bottom = Color(0xFF3D2A24),
        accent = Color(0xFF9FD0C5),
        ink = Color(0xFFFFF4EA),
    ),
)

@Composable
private fun BookshelfEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(42.dp),
            )
            Text(
                text = Strings.BOOKSHELF_EMPTY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookshelfSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(BookshelfLayoutMode.GRID_3.gridColumns ?: 3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(List(8) { it }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.68f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                )
            }
        }
    }
}
