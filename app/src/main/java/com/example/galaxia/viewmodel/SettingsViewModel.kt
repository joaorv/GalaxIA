package com.example.galaxia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.AppCardSize
import com.example.galaxia.data.local.AppFontSize
import com.example.galaxia.data.local.AppThemeMode
import com.example.galaxia.data.local.UserPreferences
import com.example.galaxia.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel reativo para as preferências de personalização da interface.
 */
class SettingsViewModel(
    private val repository: UserPreferencesRepository = UserPreferencesRepository.getInstance(
        GalaxiaApplication.instance
    )
) : BaseViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )

    fun updateFontSize(fontSize: AppFontSize) {
        viewModelScope.launch {
            repository.setFontSize(fontSize)
        }
    }

    fun updateCardSize(cardSize: AppCardSize) {
        viewModelScope.launch {
            repository.setCardSize(cardSize)
        }
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }
}
