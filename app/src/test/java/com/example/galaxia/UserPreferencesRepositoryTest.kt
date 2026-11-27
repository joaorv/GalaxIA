package com.example.galaxia

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.galaxia.data.local.AppCardSize
import com.example.galaxia.data.local.AppFontSize
import com.example.galaxia.data.local.AppThemeMode
import com.example.galaxia.data.local.UserPreferences
import com.example.galaxia.data.local.UserPreferencesRepository
import com.example.galaxia.repository.ApodRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class UserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun initialPreferences_defaultToDefaultFontAndCardSize() = runBlocking {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(tempFolder.root, "prefs_default.preferences_pb") }
            )
            val repo = UserPreferencesRepository(dataStore)

            val prefs = repo.preferencesFlow.first()
            assertEquals(UserPreferences(AppFontSize.DEFAULT, AppCardSize.DEFAULT), prefs)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun setFontSize_persistsAcrossRepositoryInstances() = runBlocking {
        val file = File(tempFolder.root, "prefs_font.preferences_pb")
        val job1 = SupervisorJob()
        val scope1 = CoroutineScope(job1 + Dispatchers.IO)
        try {
            val ds1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
            val repo1 = UserPreferencesRepository(ds1)
            repo1.setFontSize(AppFontSize.EXTRA_LARGE)
            assertEquals(AppFontSize.EXTRA_LARGE, repo1.preferencesFlow.first().fontSize)
        } finally {
            job1.cancelAndJoin()
        }

        val job2 = SupervisorJob()
        val scope2 = CoroutineScope(job2 + Dispatchers.IO)
        try {
            val ds2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
            val repo2 = UserPreferencesRepository(ds2)
            assertEquals(
                UserPreferences(AppFontSize.EXTRA_LARGE, AppCardSize.DEFAULT),
                repo2.preferencesFlow.first()
            )
        } finally {
            job2.cancelAndJoin()
        }
    }

    @Test
    fun setCardSize_persistsAcrossRepositoryInstances() = runBlocking {
        val file = File(tempFolder.root, "prefs_card.preferences_pb")
        val job1 = SupervisorJob()
        val scope1 = CoroutineScope(job1 + Dispatchers.IO)
        try {
            val ds1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
            val repo1 = UserPreferencesRepository(ds1)
            repo1.setCardSize(AppCardSize.COMPACT)
            assertEquals(AppCardSize.COMPACT, repo1.preferencesFlow.first().cardSize)
        } finally {
            job1.cancelAndJoin()
        }

        val job2 = SupervisorJob()
        val scope2 = CoroutineScope(job2 + Dispatchers.IO)
        try {
            val ds2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
            val repo2 = UserPreferencesRepository(ds2)
            assertEquals(
                UserPreferences(AppFontSize.DEFAULT, AppCardSize.COMPACT),
                repo2.preferencesFlow.first()
            )
        } finally {
            job2.cancelAndJoin()
        }
    }

    @Test
    fun sequentialUpdatesAndIoException_emitExpectedPreferences() = runBlocking {
        val state = MutableStateFlow(emptyPreferences())
        val fakeDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = state
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                val updated = transform(state.value)
                state.value = updated
                return updated
            }
        }
        val repo = UserPreferencesRepository(fakeDataStore)
        repo.setFontSize(AppFontSize.LARGE)
        repo.setCardSize(AppCardSize.LARGE)
        assertEquals(UserPreferences(AppFontSize.LARGE, AppCardSize.LARGE), repo.preferencesFlow.first())

        val failingDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("Disk error") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                emptyPreferences()
        }
        val fallbackRepo = UserPreferencesRepository(failingDataStore)
        assertEquals(UserPreferences(AppFontSize.DEFAULT, AppCardSize.DEFAULT), fallbackRepo.preferencesFlow.first())
    }

    @Test
    fun invalidStoredValues_fallbackSafelyToDefaults() = runBlocking {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(tempFolder.root, "prefs_invalid.preferences_pb") }
            )
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("font_size")] = "UNKNOWN_FONT"
                prefs[stringPreferencesKey("card_size")] = "UNKNOWN_CARD"
                prefs[stringPreferencesKey("theme_mode")] = "UNKNOWN_THEME"
            }

            val repo = UserPreferencesRepository(dataStore)
            val prefs = repo.preferencesFlow.first()
            assertEquals(AppFontSize.DEFAULT, prefs.fontSize)
            assertEquals(AppCardSize.DEFAULT, prefs.cardSize)
            assertEquals(AppThemeMode.DARK, prefs.themeMode)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun setThemeMode_persistsAcrossRepositoryInstances() = runBlocking {
        val file = File(tempFolder.root, "prefs_theme.preferences_pb")
        val job1 = SupervisorJob()
        val scope1 = CoroutineScope(job1 + Dispatchers.IO)
        try {
            val ds1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
            val repo1 = UserPreferencesRepository(ds1)
            repo1.setThemeMode(AppThemeMode.LIGHT)
            assertEquals(AppThemeMode.LIGHT, repo1.preferencesFlow.first().themeMode)
        } finally {
            job1.cancelAndJoin()
        }

        val job2 = SupervisorJob()
        val scope2 = CoroutineScope(job2 + Dispatchers.IO)
        try {
            val ds2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
            val repo2 = UserPreferencesRepository(ds2)
            assertEquals(
                UserPreferences(AppFontSize.DEFAULT, AppCardSize.DEFAULT, AppThemeMode.LIGHT),
                repo2.preferencesFlow.first()
            )
        } finally {
            job2.cancelAndJoin()
        }
    }

    @Test
    fun latestApodDate_clampsToEarliestBetweenLocalAndNasaDate() {
        val nov26 = LocalDate.of(2026, 11, 26)
        val nov27 = LocalDate.of(2026, 11, 27)

        // Emulador em UTC (dia 27) enquanto na NASA/Brasil ainda é dia 26 -> retorna dia 26
        assertEquals(nov26, ApodRepository.latestApodDate(localToday = nov27, nasaToday = nov26))
        // Usuário em fuso atrasado (dia 26) enquanto NASA já virou para dia 27 -> mantém dia 26
        assertEquals(nov26, ApodRepository.latestApodDate(localToday = nov26, nasaToday = nov27))
        // Ambos no mesmo dia -> retorna o dia atual
        assertEquals(nov27, ApodRepository.latestApodDate(localToday = nov27, nasaToday = nov27))
    }
}
