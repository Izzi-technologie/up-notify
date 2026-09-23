package com.wayscompany.webhookalarm.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.alarmSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "webhook_alarm")

class SettingsRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.alarmSettingsDataStore
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val settings: StateFlow<AppSettings> = dataStore.data
        .map { preferences ->
            _loaded.value = true
            SettingsCodec.decode(preferences.toValues())
        }
        .catch {
            _loaded.value = true
            emit(AppSettings())
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun save(settings: AppSettings) {
        dataStore.edit { preferences -> SettingsCodec.write(preferences, settings) }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val updated = transform(SettingsCodec.decode(preferences.toValues()))
            SettingsCodec.write(preferences, updated)
        }
    }

    private fun Preferences.toValues(): Map<String, String> =
        asMap().entries.associate { (key, value) -> key.name to value.toString() }
}
