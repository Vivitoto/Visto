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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    current: ReaderSession,
    onFontSize: (Int) -> Unit,
    onLineSpacing: (Float) -> Unit,
    onTheme: (ReaderTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(text = "阅读设置", style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "字号 ${current.fontSizeSp}sp", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = current.fontSizeSp.toFloat(),
                    onValueChange = { onFontSize(it.roundToInt()) },
                    valueRange = 14f..28f,
                    steps = 13,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "行距", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LineSpacingChip("紧凑", 1.2f, current.lineSpacing, onLineSpacing)
                    LineSpacingChip("标准", 1.5f, current.lineSpacing, onLineSpacing)
                    LineSpacingChip("宽松", 2.0f, current.lineSpacing, onLineSpacing)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "主题", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeChip("白天", ReaderTheme.LIGHT, current.theme, onTheme)
                    ThemeChip("夜间", ReaderTheme.DARK, current.theme, onTheme)
                    ThemeChip("护眼", ReaderTheme.CREAM, current.theme, onTheme)
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
                        text = "预览",
                        style = MaterialTheme.typography.labelMedium,
                        color = current.theme.textColor.copy(alpha = 0.68f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "这是 Visto 阅读器的文字预览。调整字号、行距和主题后，会实时应用到这里。",
                        color = current.theme.textColor,
                        fontSize = current.fontSizeSp.sp,
                        lineHeight = (current.fontSizeSp * current.lineSpacing).sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
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
