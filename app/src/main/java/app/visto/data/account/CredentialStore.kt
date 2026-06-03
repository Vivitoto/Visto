package app.visto.data.account

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores WebDAV credentials in an Android Keystore-backed
 * [EncryptedSharedPreferences] file, keyed by a stable [credentialRef] that
 * also lives in [app.visto.data.db.DavAccountEntity.credentialRef].
 *
 * Visto's account row never holds the plain password. The credentialRef is
 * the only piece of bridging metadata stored unencrypted.
 */
class CredentialStore(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun savePassword(credentialRef: String, password: String) {
        prefs.edit().putString(credentialRef, password).apply()
    }

    fun loadPassword(credentialRef: String): String? = prefs.getString(credentialRef, null)

    fun deletePassword(credentialRef: String) {
        prefs.edit().remove(credentialRef).apply()
    }

    companion object {
        private const val FILE_NAME = "visto_credentials"

        fun newCredentialRef(): String = "cred-" + java.util.UUID.randomUUID().toString()
    }
}
