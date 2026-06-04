package app.visto.data.account

import android.content.Context
import android.content.SharedPreferences
import app.visto.ui.theme.ThemeMode
import androidx.core.content.edit

/**
 * Thin preference wrapper for user-facing settings that survive restarts.
 *
 * Co-located with AccountService so preferences are scoped to the app's
 * private space, not leaked to any other process.
 */
class VistoPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromStorage(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.storageKey))
        set(mode) = prefs.edit { putString(KEY_THEME, mode.storageKey) }

    companion object {
        private const val PREF_NAME = "visto_settings"
        private const val KEY_THEME = "theme_mode"
    }
}