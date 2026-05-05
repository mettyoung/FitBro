package com.mettyoung.fitbro.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.mettyoung.fitbro.AndroidAppContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual fun createTokenStorage(): TokenStorage = AndroidTokenStorage(AndroidAppContext.context)

private class AndroidTokenStorage(context: Context) : TokenStorage {
    private val prefs: SharedPreferences by lazy {
        val keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "fitbro_oauth_prefs",
            keyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveToken(token: OAuthToken) {
        prefs.edit().putString(KEY_TOKEN, Json.encodeToString(token)).apply()
    }

    override fun loadToken(): OAuthToken? =
        prefs.getString(KEY_TOKEN, null)?.let {
            try { Json.decodeFromString<OAuthToken>(it) } catch (e: Exception) { null }
        }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "oauth_token"
    }
}
