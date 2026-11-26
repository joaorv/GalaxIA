package com.example.galaxia.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contrato de armazenamento local para a chave de API da NASA configurada pelo usuário.
 */
interface ApiKeyStorage {
    fun getCustomKey(): String?
    fun setCustomKey(key: String?)
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(dismissed: Boolean)
}

class SharedPreferencesApiKeyStorage(context: Context) : ApiKeyStorage {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getCustomKey(): String? = prefs.getString(KEY_CUSTOM_API_KEY, null)

    override fun setCustomKey(key: String?) {
        val editor = prefs.edit()
        if (key.isNullOrBlank()) {
            editor.remove(KEY_CUSTOM_API_KEY)
        } else {
            editor.putString(KEY_CUSTOM_API_KEY, key)
        }
        editor.apply()
    }

    override fun isPromptDismissed(): Boolean = prefs.getBoolean(KEY_PROMPT_DISMISSED, false)

    override fun setPromptDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_PROMPT_DISMISSED, dismissed).apply()
    }

    private companion object {
        const val PREFS_NAME = "galaxia_api_key_prefs"
        const val KEY_CUSTOM_API_KEY = "custom_nasa_api_key"
        const val KEY_PROMPT_DISMISSED = "api_key_prompt_dismissed"
    }
}

/**
 * Gerenciador central da chave de API da NASA.
 * Por padrão utiliza [DEMO_KEY] (30 req/hora e 50 req/dia) e permite que o usuário
 * insira sua própria chave obtida em https://api.nasa.gov/ (até 1.000 req/hora).
 */
class NasaApiKeyManager(
    private val storage: ApiKeyStorage
) {

    private val _customApiKey = MutableStateFlow(
        storage.getCustomKey()?.trim()?.takeIf { it.isNotBlank() }
    )
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    private val _isUsingCustomKey = MutableStateFlow(_customApiKey.value != null)
    val isUsingCustomKey: StateFlow<Boolean> = _isUsingCustomKey.asStateFlow()

    private val _shouldShowOnboardingPrompt = MutableStateFlow(
        _customApiKey.value == null && !storage.isPromptDismissed()
    )
    val shouldShowOnboardingPrompt: StateFlow<Boolean> = _shouldShowOnboardingPrompt.asStateFlow()

    fun getActiveApiKey(): String =
        _customApiKey.value?.takeIf { it.isNotBlank() } ?: DEMO_KEY

    fun saveApiKey(rawKey: String): Boolean {
        val trimmed = rawKey.trim()
        if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) {
            return false
        }
        storage.setCustomKey(trimmed)
        storage.setPromptDismissed(true)
        _customApiKey.value = trimmed
        _isUsingCustomKey.value = true
        _shouldShowOnboardingPrompt.value = false
        return true
    }

    fun useDemoKey() {
        storage.setCustomKey(null)
        storage.setPromptDismissed(true)
        _customApiKey.value = null
        _isUsingCustomKey.value = false
        _shouldShowOnboardingPrompt.value = false
    }

    fun dismissPrompt() {
        storage.setPromptDismissed(true)
        _shouldShowOnboardingPrompt.value = false
    }

    companion object {
        const val DEMO_KEY = "DEMO_KEY"
        const val NASA_API_PORTAL_URL = "https://api.nasa.gov/"

        @Volatile
        private var INSTANCE: NasaApiKeyManager? = null

        fun getInstance(context: Context): NasaApiKeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NasaApiKeyManager(
                    SharedPreferencesApiKeyStorage(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
}
