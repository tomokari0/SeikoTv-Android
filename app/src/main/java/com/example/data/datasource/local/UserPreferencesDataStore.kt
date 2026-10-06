package com.example.data.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "seiko_preferences")

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val KEY_ACTIVE_PROFILE_NAME = stringPreferencesKey("active_profile_name")
        val KEY_AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val KEY_DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
        val KEY_HAS_COMPLETED_SPLASH = booleanPreferencesKey("has_completed_splash")
    }

    val activeProfileId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PROFILE_ID] ?: "profile_1"
    }

    val activeProfileName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PROFILE_NAME] ?: "Seiko"
    }

    val autoSkipIntro: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SKIP_INTRO] ?: false
    }

    suspend fun setActiveProfile(profileId: String, profileName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_PROFILE_ID] = profileId
            preferences[KEY_ACTIVE_PROFILE_NAME] = profileName
        }
    }

    suspend fun setAutoSkipIntro(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SKIP_INTRO] = enabled
        }
    }
}
