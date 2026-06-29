package app.visto.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.visto.core.book.Chapter
import app.visto.ui.Strings

internal const val CHAPTER_LIST_SHEET_GESTURES_ENABLED = false

@Composable
fun ChapterListSheet(
    chapters: List<Chapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matchingIndices = remember(chapters, query) { matchingChapterIndices(chapters, query) }

    NonDraggableChapterBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
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
                    .weight(1f, fill = false),
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
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = rowColors.containerColor,
                            headlineColor = rowColors.headlineColor,
                            supportingColor = rowColors.supportingColor,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            },
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

@Composable
private fun NonDraggableChapterBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val sheetInteractionSource = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sheetMaxHeight = minOf(maxHeight * 0.9f, 680.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = scrimInteractionSource,
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .imePadding()
                    .clickable(
                        interactionSource = sheetInteractionSource,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    content()
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

internal data class ChapterListRowColors(
    val containerColor: Color,
    val headlineColor: Color,
    val supportingColor: Color,
)

internal fun chapterListRowColors(selected: Boolean, colorScheme: ColorScheme): ChapterListRowColors =
    if (selected) {
        ChapterListRowColors(
            containerColor = colorScheme.primaryContainer,
            headlineColor = colorScheme.onPrimaryContainer,
            supportingColor = colorScheme.onPrimaryContainer,
        )
    } else {
        ChapterListRowColors(
            containerColor = colorScheme.surface,
            headlineColor = colorScheme.onSurface,
            supportingColor = colorScheme.onSurfaceVariant,
        )
    }
