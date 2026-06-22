package app.visto.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.core.book.Chapter
import app.visto.ui.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    chapters: List<Chapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matchingIndices = remember(chapters, query) { matchingChapterIndices(chapters, query) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Strings.READER_CHAPTERS,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp),
                singleLine = true,
                label = { Text(Strings.READER_CHAPTER_SEARCH) },
                placeholder = { Text(Strings.READER_CHAPTER_SEARCH_PLACEHOLDER) },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
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
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            },
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = chapter.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            supportingContent = {
                                Text(text = Strings.readerChapterNumber(index))
                            },
                        )
                    }
                }
            }
        }
    }
}

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
