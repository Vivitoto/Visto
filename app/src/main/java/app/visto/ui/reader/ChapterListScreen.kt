package app.visto.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.core.book.Chapter
import app.visto.ui.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    chapters: List<Chapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matchingIndices = remember(chapters, query) { matchingChapterIndices(chapters, query) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialChapterListScrollIndex(
            chapterCount = chapters.size,
            currentIndex = currentIndex,
        ),
    )

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.READER_CHAPTERS) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.BACK,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
                singleLine = true,
                label = { Text(Strings.READER_CHAPTER_SEARCH) },
                placeholder = { Text(Strings.READER_CHAPTER_SEARCH_PLACEHOLDER) },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (matchingIndices.isEmpty()) {
                    item {
                        Text(
                            text = Strings.READER_CHAPTER_SEARCH_EMPTY,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                        )
                    }
                }
                items(matchingIndices, key = { it }) { index ->
                    val chapter = chapters[index]
                    val selected = index == currentIndex
                    val rowColors = chapterListRowColors(selected, MaterialTheme.colorScheme)
                    Text(
                        text = chapter.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = rowColors.headlineColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowColors.containerColor)
                            .clickable {
                                onSelect(index)
                                onBack()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

internal fun initialChapterListScrollIndex(chapterCount: Int, currentIndex: Int): Int =
    if (chapterCount <= 0) 0 else currentIndex.coerceIn(0, chapterCount - 1)

internal fun matchingChapterIndices(chapters: List<Chapter>, query: String): List<Int> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return chapters.indices.toList()

    val digitQuery = normalizedQuery.filter { it.isDigit() }
    return chapters.mapIndexedNotNull { index, chapter ->
        val chapterNumber = (index + 1).toString()
        val matchesTitle = chapter.title.contains(normalizedQuery, ignoreCase = true)
        val matchesNumber = chapterNumber.contains(normalizedQuery) ||
            (digitQuery.isNotEmpty() && chapterNumber.contains(digitQuery))
        if (matchesTitle || matchesNumber) index else null
    }
}

internal data class ChapterListRowColors(
    val containerColor: Color,
    val headlineColor: Color,
)

internal fun chapterListRowColors(selected: Boolean, colorScheme: ColorScheme): ChapterListRowColors =
    if (selected) {
        ChapterListRowColors(
            containerColor = colorScheme.primaryContainer,
            headlineColor = colorScheme.onPrimaryContainer,
        )
    } else {
        ChapterListRowColors(
            containerColor = colorScheme.surface,
            headlineColor = colorScheme.onSurface,
        )
    }
