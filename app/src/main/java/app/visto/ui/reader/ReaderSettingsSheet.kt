package app.visto.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    fontSizeSp: Int,
    lineSpacing: Float,
    theme: ReaderTheme,
    onDismiss: () -> Unit,
    onApply: (fontSizeSp: Int, lineSpacing: Float, theme: ReaderTheme) -> Unit,
) {
    var selectedFontSize by remember(fontSizeSp) { mutableFloatStateOf(fontSizeSp.toFloat()) }
    var selectedLineSpacing by remember(lineSpacing) { mutableFloatStateOf(lineSpacing) }
    var selectedTheme by remember(theme) { mutableStateOf(theme) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "字号 ${selectedFontSize.roundToInt()}sp",
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = selectedFontSize,
                    onValueChange = { selectedFontSize = it },
                    valueRange = 14f..28f,
                    steps = 13,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "行距", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LineSpacingOption("紧凑", 1.2f, selectedLineSpacing) { selectedLineSpacing = it }
                    LineSpacingOption("标准", 1.5f, selectedLineSpacing) { selectedLineSpacing = it }
                    LineSpacingOption("宽松", 2.0f, selectedLineSpacing) { selectedLineSpacing = it }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "主题", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOption("白天", ReaderTheme.LIGHT, selectedTheme) { selectedTheme = it }
                    ThemeOption("夜间", ReaderTheme.DARK, selectedTheme) { selectedTheme = it }
                    ThemeOption("护眼", ReaderTheme.CREAM, selectedTheme) { selectedTheme = it }
                }
            }

            val colors = selectedTheme.colors()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.background)
                    .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "预览",
                    color = colors.text,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "这是阅读效果预览，字号、行距和主题会在应用后生效。",
                    color = colors.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = selectedFontSize.roundToInt().sp,
                        lineHeight = (selectedFontSize.roundToInt() * selectedLineSpacing).sp,
                    ),
                )
            }

            Button(
                onClick = {
                    onApply(selectedFontSize.roundToInt(), selectedLineSpacing, selectedTheme)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("应用")
            }
        }
    }
}

@Composable
private fun RowScope.LineSpacingOption(
    label: String,
    value: Float,
    selected: Float,
    onSelect: (Float) -> Unit,
) {
    val isSelected = kotlin.math.abs(value - selected) < 0.05f
    if (isSelected) {
        Button(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label) }
    } else {
        OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label) }
    }
}

@Composable
private fun RowScope.ThemeOption(
    label: String,
    value: ReaderTheme,
    selected: ReaderTheme,
    onSelect: (ReaderTheme) -> Unit,
) {
    val colors = value.colors()
    val borderColor = if (value == selected) MaterialTheme.colorScheme.primary else colors.divider
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect(value) }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .border(1.dp, colors.divider, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.text),
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
