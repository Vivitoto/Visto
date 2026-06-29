package app.visto.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visto.ui.Strings
import kotlin.math.roundToInt

private val ReaderSheetHorizontalPadding = 18.dp
private val ReaderSheetBottomPadding = 24.dp
private val ReaderSheetSectionGap = 12.dp
private val ReaderSheetItemGap = 8.dp
private val ReaderSheetChipHorizontalGap = 8.dp
private val ReaderSheetChipVerticalGap = 6.dp
private val ReaderSheetPreviewPadding = 14.dp

@Composable
fun ReaderSettingsSheet(
    current: ReaderSession,
    onFontSize: (Int) -> Unit,
    onLineSpacing: (Float) -> Unit,
    onFontChoice: (ReaderFontChoice) -> Unit,
    onImportFont: () -> Unit,
    onTheme: (ReaderTheme) -> Unit,
    onTextColor: (ReaderTextColor) -> Unit,
    onBackgroundStyle: (ReaderBackgroundStyle) -> Unit,
    onPageMarginTop: (Int) -> Unit,
    onPageMarginBottom: (Int) -> Unit,
    onPageMarginStart: (Int) -> Unit,
    onPageMarginEnd: (Int) -> Unit,
    onSetDefaultSettings: () -> Unit,
    onDismiss: () -> Unit,
    fontImportError: String? = null,
) {
    val previewFontFamily = rememberReaderFontFamily(current.fontChoice)
    val previewPalette = current.readerPalette()

    VistoBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = ReaderSheetHorizontalPadding,
                    end = ReaderSheetHorizontalPadding,
                    bottom = ReaderSheetBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(ReaderSheetSectionGap),
        ) {
            Text(text = Strings.READER_SETTINGS, style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.readerFontSize(current.fontSizeSp), style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = current.fontSizeSp.toFloat(),
                    onValueChange = { onFontSize(it.roundToInt()) },
                    valueRange = 14f..28f,
                    steps = 13,
                    colors = readerSliderColors(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.READER_FONT, style = MaterialTheme.typography.titleSmall)
                ChipFlowRow {
                    ReaderFontChoice.BUILT_IN.forEach { choice ->
                        FontChip(readerFontLabel(choice), choice, current.fontChoice, onFontChoice)
                    }
                    if (current.fontChoice is ReaderFontChoice.Custom) {
                        FontChip(
                            label = readerFontLabel(current.fontChoice),
                            value = current.fontChoice,
                            current = current.fontChoice,
                            onFontChoice = onFontChoice,
                        )
                    }
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

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(
                    text = Strings.readerLineSpacing(current.lineSpacing),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = current.lineSpacing.coerceIn(1.0f, 2.4f),
                    onValueChange = { value ->
                        onLineSpacing((value * 10f).roundToInt() / 10f)
                    },
                    valueRange = 1.0f..2.4f,
                    steps = 13,
                    colors = readerSliderColors(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.READER_PAGE_MARGINS, style = MaterialTheme.typography.titleSmall)
                ReaderMarginSlider(
                    label = Strings.READER_MARGIN_TOP,
                    value = ReaderPageMargins.topExtraDp(current.pageMargins.topDp),
                    minDp = 0,
                    maxDp = ReaderPageMargins.EXTRA_MAX_DP,
                    onValueChange = { extra -> onPageMarginTop(ReaderPageMargins.TOP_BASELINE_DP + extra) },
                )
                ReaderMarginSlider(
                    label = Strings.READER_MARGIN_BOTTOM,
                    value = ReaderPageMargins.bottomExtraDp(current.pageMargins.bottomDp),
                    minDp = 0,
                    maxDp = ReaderPageMargins.EXTRA_MAX_DP,
                    onValueChange = { extra -> onPageMarginBottom(ReaderPageMargins.BOTTOM_BASELINE_DP + extra) },
                )
                ReaderMarginSlider(
                    label = Strings.READER_MARGIN_START,
                    value = ReaderPageMargins.horizontalExtraDp(current.pageMargins.startDp),
                    minDp = 0,
                    maxDp = ReaderPageMargins.EXTRA_MAX_DP,
                    onValueChange = { extra -> onPageMarginStart(ReaderPageMargins.HORIZONTAL_BASELINE_DP + extra) },
                )
                ReaderMarginSlider(
                    label = Strings.READER_MARGIN_END,
                    value = ReaderPageMargins.horizontalExtraDp(current.pageMargins.endDp),
                    minDp = 0,
                    maxDp = ReaderPageMargins.EXTRA_MAX_DP,
                    onValueChange = { extra -> onPageMarginEnd(ReaderPageMargins.HORIZONTAL_BASELINE_DP + extra) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.READER_THEME, style = MaterialTheme.typography.titleSmall)
                ChipFlowRow {
                    ThemeChip(Strings.READER_THEME_LIGHT, ReaderTheme.LIGHT, current.theme, onTheme)
                    ThemeChip(Strings.READER_THEME_DARK, ReaderTheme.DARK, current.theme, onTheme)
                    ThemeChip(Strings.READER_THEME_CREAM, ReaderTheme.CREAM, current.theme, onTheme)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.READER_TEXT_COLOR, style = MaterialTheme.typography.titleSmall)
                ChipFlowRow {
                    ReaderTextColor.entries.forEach { color ->
                        TextColorChip(color, current.textColor, onTextColor)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ReaderSheetItemGap)) {
                Text(text = Strings.READER_BACKGROUND_STYLE, style = MaterialTheme.typography.titleSmall)
                ChipFlowRow {
                    ReaderBackgroundStyle.entries.forEach { background ->
                        BackgroundStyleChip(background, current.backgroundStyle, onBackgroundStyle)
                    }
                }
            }

            Surface(
                color = previewPalette.backgroundColor,
                contentColor = previewPalette.textColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ReaderSheetPreviewPadding),
                ) {
                    Text(
                        text = Strings.READER_PREVIEW,
                        style = MaterialTheme.typography.labelMedium,
                        color = previewPalette.textColor.copy(alpha = 0.68f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.READER_PREVIEW_TEXT,
                        color = previewPalette.textColor,
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
private fun ReaderMarginSlider(
    label: String,
    value: Int,
    minDp: Int = 0,
    maxDp: Int = ReaderPageMargins.EXTRA_MAX_DP,
    onValueChange: (Int) -> Unit,
) {
    val clamped = value.coerceIn(minDp, maxDp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReaderSheetItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Strings.readerMarginDp(clamped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = clamped.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = minDp.toFloat()..maxDp.toFloat(),
            steps = maxDp - minDp - 1,
            colors = readerSliderColors(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlowRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReaderSheetChipHorizontalGap),
        verticalArrangement = Arrangement.spacedBy(ReaderSheetChipVerticalGap),
    ) {
        content()
    }
}

@Composable
private fun ChipLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FontChip(
    label: String,
    value: ReaderFontChoice,
    current: ReaderFontChoice,
    onFontChoice: (ReaderFontChoice) -> Unit,
) {
    ReaderSettingsChip(
        selected = current == value,
        onClick = { onFontChoice(value) },
        label = { ChipLabel(label) },
    )
}

private fun readerFontLabel(choice: ReaderFontChoice): String = when (choice) {
    ReaderFontChoice.SystemDefault -> Strings.READER_FONT_SYSTEM
    ReaderFontChoice.Sans -> Strings.READER_FONT_SANS
    ReaderFontChoice.Serif -> Strings.READER_FONT_SERIF
    is ReaderFontChoice.Custom -> Strings.readerCustomFont(choice.fileName.substringBeforeLast('.', choice.fileName))
}

@Composable
private fun TextColorChip(
    value: ReaderTextColor,
    current: ReaderTextColor,
    onTextColor: (ReaderTextColor) -> Unit,
) {
    ReaderSettingsChip(
        selected = current == value,
        onClick = { onTextColor(value) },
        label = { ChipLabel(value.displayLabel) },
    )
}

@Composable
private fun BackgroundStyleChip(
    value: ReaderBackgroundStyle,
    current: ReaderBackgroundStyle,
    onBackgroundStyle: (ReaderBackgroundStyle) -> Unit,
) {
    ReaderSettingsChip(
        selected = current == value,
        onClick = { onBackgroundStyle(value) },
        label = { ChipLabel(value.displayLabel) },
    )
}

@Composable
private fun ThemeChip(
    label: String,
    value: ReaderTheme,
    current: ReaderTheme,
    onTheme: (ReaderTheme) -> Unit,
) {
    ReaderSettingsChip(
        selected = current == value,
        onClick = { onTheme(value) },
        label = { ChipLabel(label) },
        leadingIcon = {
            Surface(
                color = value.backgroundColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
                modifier = Modifier.size(16.dp),
            ) {
                Spacer(modifier = Modifier.size(16.dp))
            }
        },
    )
}

@Composable
private fun ReaderSettingsChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (leadingIcon == null) 12.dp else 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            label()
        }
    }
}

@Composable
private fun readerSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
)
