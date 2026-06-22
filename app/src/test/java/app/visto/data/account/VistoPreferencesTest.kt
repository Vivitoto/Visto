package app.visto.data.account

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        )

        val reloaded = VistoPreferences(context).defaultReaderSettings
        assertEquals(28, reloaded.fontSizeSp)
        assertEquals(2.4f, reloaded.lineSpacing, 0.0f)
        assertEquals("dark", reloaded.theme)
        assertEquals("serif", reloaded.fontChoice)
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

    private companion object {
        const val PREF_NAME = "visto_settings"
        const val KEY_READER_DEFAULT_THEME = "reader_default_theme"
        const val KEY_READER_DEFAULT_FONT_CHOICE = "reader_default_font_choice"
    }
}
