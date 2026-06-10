package com.example.gymstruck.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.WorkoutConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preset")

interface PresetRepository {
    val preset: Flow<WorkoutConfig>
    suspend fun save(config: WorkoutConfig)
}

class DataStorePresetRepository(context: Context) : PresetRepository {

    private val dataStore = context.dataStore
    private val KEY_TOTAL_SETS    = intPreferencesKey("total_sets")
    private val KEY_REST_MILLIS   = longPreferencesKey("rest_millis")
    private val KEY_REST_MODE     = stringPreferencesKey("rest_mode")
    private val KEY_SEARCH_QUERY  = stringPreferencesKey("search_query")

    override val preset: Flow<WorkoutConfig> = dataStore.data.map { prefs ->
        WorkoutConfig(
            totalSets   = prefs[KEY_TOTAL_SETS]  ?: 3,
            restMillis  = prefs[KEY_REST_MILLIS] ?: 90_000L,
            restMode    = prefs[KEY_REST_MODE]?.let { runCatching { RestMode.valueOf(it) }.getOrNull() }
                ?: RestMode.DUCKING,
            searchQuery = prefs[KEY_SEARCH_QUERY] ?: "",
        )
    }

    override suspend fun save(config: WorkoutConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_TOTAL_SETS]   = config.totalSets
            prefs[KEY_REST_MILLIS]  = config.restMillis
            prefs[KEY_REST_MODE]    = config.restMode.name
            prefs[KEY_SEARCH_QUERY] = config.searchQuery
        }
    }
}
