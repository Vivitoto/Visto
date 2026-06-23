package app.visto.data.account

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.visto.ui.reader.ReaderPageMargins
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class VistoPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultReaderSettingsUseReaderDefaults() {
        val prefs = VistoPreferences(context)

        assertEquals(
            ReaderDefaultSettings(
                fontSizeSp = 18,
                lineSpacing = 1.5f,
                theme = "light",
                fontChoice = "system",
                textColor = "default",
                backgroundStyle = "default",
                pageMargins = ReaderPageMargins.DEFAULT,
            ),
            prefs.defaultReaderSettings,
        )
    }

    @Test
    fun defaultReaderSettingsPersistAndClampToSupportedRanges() {
        val prefs = VistoPreferences(context)

        prefs.defaultReaderSettings = ReaderDefaultSettings(
            fontSizeSp = 40,
            lineSpacing = 5.0f,
            theme = "DARK",
            fontChoice = "serif",
            textColor = "warm_brown",
            backgroundStyle = "night",
            pageMargins = ReaderPageMargins(
                topDp = 2,
                bottomDp = 500,
                startDp = 36,
                endDp = 42,
            ),
        )

        val reloaded = VistoPreferences(context).defaultReaderSettings
        assertEquals(28, reloaded.fontSizeSp)
        assertEquals(2.4f, reloaded.lineSpacing, 0.0f)
        assertEquals("dark", reloaded.theme)
        assertEquals("serif", reloaded.fontChoice)
        assertEquals("warm_brown", reloaded.textColor)
        assertEquals("night", reloaded.backgroundStyle)
        assertEquals(
            ReaderPageMargins(
                topDp = ReaderPageMargins.MIN_DP,
                bottomDp = ReaderPageMargins.MAX_DP,
                startDp = 36,
                endDp = 42,
            ),
            reloaded.pageMargins,
        )
    }

    @Test
    fun malformedStoredReaderThemeFallsBackToLight() {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_DEFAULT_THEME, "sepia")
            .commit()

        val prefs = VistoPreferences(context)

        assertEquals("light", prefs.defaultReaderSettings.theme)
    }

    @Test
    fun malformedStoredReaderFontFallsBackToSystem() {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_DEFAULT_FONT_CHOICE, "custom:../bad.ttf")
            .commit()

        val prefs = VistoPreferences(context)

        assertEquals("system", prefs.defaultReaderSettings.fontChoice)
    }

    @Test
    fun malformedStoredReaderColorsFallBackToDefaults() {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_DEFAULT_TEXT_COLOR, "neon")
            .putString(KEY_READER_DEFAULT_BACKGROUND_STYLE, "wallpaper")
            .commit()

        val prefs = VistoPreferences(context)

        assertEquals("default", prefs.defaultReaderSettings.textColor)
        assertEquals("default", prefs.defaultReaderSettings.backgroundStyle)
    }

    private companion object {
        const val PREF_NAME = "visto_settings"
        const val KEY_READER_DEFAULT_THEME = "reader_default_theme"
        const val KEY_READER_DEFAULT_FONT_CHOICE = "reader_default_font_choice"
        const val KEY_READER_DEFAULT_TEXT_COLOR = "reader_default_text_color"
        const val KEY_READER_DEFAULT_BACKGROUND_STYLE = "reader_default_background_style"
    }
}
