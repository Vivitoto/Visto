package app.visto.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visto.ui.Strings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    current: ReaderSession,
    onFontSize: (Int) -> Unit,
    onLineSpacing: (Float) -> Unit,
    onFontChoice: (ReaderFontChoice) -> Unit,
    onImportFont: () -> Unit,
    onTheme: (ReaderTheme) -> Unit,
    onSetDefaultSettings: () -> Unit,
    onDismiss: () -> Unit,
    fontImportError: String? = null,
) {
    val previewFontFamily = rememberReaderFontFamily(current.fontChoice)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(text = Strings.READER_SETTINGS, style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = Strings.readerFontSize(current.fontSizeSp), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = current.fontSizeSp.toFloat(),
                    onValueChange = { onFontSize(it.roundToInt()) },
                    valueRange = 14f..28f,
                    steps = 13,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = Strings.READER_FONT, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReaderFontChoice.BUILT_IN.forEach { choice ->
                        FontChip(readerFontLabel(choice), choice, current.fontChoice, onFontChoice)
                    }
                }
                if (current.fontChoice is ReaderFontChoice.Custom) {
                    FontChip(
                        label = readerFontLabel(current.fontChoice),
                        value = current.fontChoice,
                        current = current.fontChoice,
                        onFontChoice = onFontChoice,
                    )
                }
                OutlinedButton(
                    onClick = onImportFont,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(Strings.READER_FONT_IMPORT)
                }
                if (fontImportError != null) {
                    Text(
                        text = fontImportError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = Strings.READER_LINE_SPACING, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LineSpacingChip(Strings.READER_LINE_SPACING_COMPACT, 1.2f, current.lineSpacing, onLineSpacing)
                    LineSpacingChip(Strings.READER_LINE_SPACING_STANDARD, 1.5f, current.lineSpacing, onLineSpacing)
                    LineSpacingChip(Strings.READER_LINE_SPACING_RELAXED, 2.0f, current.lineSpacing, onLineSpacing)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = Strings.READER_THEME, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeChip(Strings.READER_THEME_LIGHT, ReaderTheme.LIGHT, current.theme, onTheme)
                    ThemeChip(Strings.READER_THEME_DARK, ReaderTheme.DARK, current.theme, onTheme)
                    ThemeChip(Strings.READER_THEME_CREAM, ReaderTheme.CREAM, current.theme, onTheme)
                }
            }

            Surface(
                color = current.theme.backgroundColor,
                contentColor = current.theme.textColor,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Text(
                        text = Strings.READER_PREVIEW,
                        style = MaterialTheme.typography.labelMedium,
                        color = current.theme.textColor.copy(alpha = 0.68f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.READER_PREVIEW_TEXT,
                        color = current.theme.textColor,
                        fontSize = current.fontSizeSp.sp,
                        lineHeight = (current.fontSizeSp * current.lineSpacing).sp,
                        fontFamily = previewFontFamily,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            OutlinedButton(
                onClick = onSetDefaultSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Strings.READER_SET_DEFAULT_SETTINGS)
            }
        }
    }
}

@Composable
private fun FontChip(
    label: String,
    value: ReaderFontChoice,
    current: ReaderFontChoice,
    onFontChoice: (ReaderFontChoice) -> Unit,
) {
    FilterChip(
        selected = current == value,
        onClick = { onFontChoice(value) },
        label = { Text(label) },
    )
}

private fun readerFontLabel(choice: ReaderFontChoice): String = when (choice) {
    ReaderFontChoice.SystemDefault -> Strings.READER_FONT_SYSTEM
    ReaderFontChoice.Sans -> Strings.READER_FONT_SANS
    ReaderFontChoice.Serif -> Strings.READER_FONT_SERIF
    is ReaderFontChoice.Custom -> Strings.readerCustomFont(choice.fileName.substringBeforeLast('.', choice.fileName))
}

@Composable
private fun LineSpacingChip(
    label: String,
    value: Float,
    current: Float,
    onLineSpacing: (Float) -> Unit,
) {
    FilterChip(
        selected = kotlin.math.abs(current - value) < 0.05f,
        onClick = { onLineSpacing(value) },
        label = { Text(label) },
    )
}

@Composable
private fun ThemeChip(
    label: String,
    value: ReaderTheme,
    current: ReaderTheme,
    onTheme: (ReaderTheme) -> Unit,
) {
    FilterChip(
        selected = current == value,
        onClick = { onTheme(value) },
        label = { Text(label) },
        leadingIcon = {
            Surface(
                color = value.backgroundColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(end = 2.dp)
                    .background(value.backgroundColor),
            ) {
                Spacer(modifier = Modifier.padding(6.dp))
            }
        },
    )
}
