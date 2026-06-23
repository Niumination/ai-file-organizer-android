package com.arena.aifileorganizer.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "ai_organizer_secure", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String?> = _apiKeyFlow
    fun getApiKey(): String? = prefs.getString("gemini_api_key", null)
    fun saveApiKey(key: String) { prefs.edit().putString("gemini_api_key", key.trim()).apply(); _apiKeyFlow.value = key.trim() }
    fun clear() { prefs.edit().remove("gemini_api_key").apply(); _apiKeyFlow.value = null }
    fun hasKey(): Boolean = !getApiKey().isNullOrBlank()
}
