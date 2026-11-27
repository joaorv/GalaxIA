package com.example.galaxia.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class AppFontSize(val label: String, val scale: Float) {
    SMALL("Pequeno", 0.88f),
    DEFAULT("Padrão", 1.00f),
    LARGE("Grande", 1.14f),
    EXTRA_LARGE("Muito grande", 1.28f);

    companion object {
        fun fromName(name: String?): AppFontSize =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class AppCardSize(val label: String, val description: String) {
    COMPACT("Compacto", "Cards menores com mais conteúdo visível na tela"),
    DEFAULT("Padrão", "Tamanho equilibrado entre imagem e texto"),
    LARGE("Grande", "Cards maiores com destaque para imagens e leitura");

    companion object {
        fun fromName(name: String?): AppCardSize =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class AppThemeMode(val label: String, val description: String) {
    DARK("Escuro", "Visual espacial clássico com fundo escuro"),
    LIGHT("Claro", "Interface clara com alto contraste para ambientes iluminados"),
    SYSTEM("Automático (Sistema)", "Acompanha o tema claro ou escuro do dispositivo");

    companion object {
        fun fromName(name: String?): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: DARK
    }
}

data class UserPreferences(
    val fontSize: AppFontSize = AppFontSize.DEFAULT,
    val cardSize: AppCardSize = AppCardSize.DEFAULT,
    val themeMode: AppThemeMode = AppThemeMode.DARK
)

private val Context.galaxiaUserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "galaxia_user_preferences"
)

/**
 * Repositório local baseado em DataStore Preferences para persistir preferências
 * de aparência do aplicativo (tamanho da fonte e tamanho dos cards).
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                fontSize = AppFontSize.fromName(prefs[KEY_FONT_SIZE]),
                cardSize = AppCardSize.fromName(prefs[KEY_CARD_SIZE]),
                themeMode = AppThemeMode.fromName(prefs[KEY_THEME_MODE])
            )
        }

    suspend fun setFontSize(fontSize: AppFontSize) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = fontSize.name
        }
    }

    suspend fun setCardSize(cardSize: AppCardSize) {
        dataStore.edit { prefs ->
            prefs[KEY_CARD_SIZE] = cardSize.name
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode.name
        }
    }

    companion object {
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_CARD_SIZE = stringPreferencesKey("card_size")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(
                    context.applicationContext.galaxiaUserPreferencesDataStore
                ).also { INSTANCE = it }
            }
        }
    }
}
