package app.visto.ui.reader

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import java.io.File

@Composable
internal fun rememberReaderFontFamily(choice: ReaderFontChoice): FontFamily? {
    val context = LocalContext.current
    return remember(choice, context.filesDir.absolutePath) {
        choice.toFontFamily(context)
    }
}

internal fun readerFontDirectory(context: Context): File =
    File(context.filesDir, READER_FONT_DIR_NAME)

internal fun ReaderFontChoice.toFontFamily(context: Context): FontFamily? = when (this) {
    ReaderFontChoice.SystemDefault -> null
    ReaderFontChoice.Sans -> FontFamily.SansSerif
    ReaderFontChoice.Serif -> FontFamily.Serif
    is ReaderFontChoice.Custom -> {
        val file = File(readerFontDirectory(context), fileName)
        if (!file.isFile) {
            null
        } else {
            runCatching { FontFamily(Typeface.createFromFile(file)) }.getOrNull()
        }
    }
}

private const val READER_FONT_DIR_NAME = "reader_fonts"
