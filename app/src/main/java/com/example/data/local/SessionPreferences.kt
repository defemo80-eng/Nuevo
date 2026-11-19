package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.detflixDataStore: DataStore<Preferences> by preferencesDataStore(name = "detflix_session_prefs")

class SessionPreferences(private val context: Context) {
    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val CURRENT_PROFILE_ID = stringPreferencesKey("current_profile_id")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val CLOUD_GATEWAY_URL = stringPreferencesKey("cloud_gateway_url")
        val REMOTE_CATALOG_URL = stringPreferencesKey("remote_catalog_url")
    }

    val currentUserId: Flow<String?> = context.detflixDataStore.data.map { it[Keys.CURRENT_USER_ID] }
    val currentProfileId: Flow<String?> = context.detflixDataStore.data.map { it[Keys.CURRENT_PROFILE_ID] }
    val lastSyncAt: Flow<Long> = context.detflixDataStore.data.map { it[Keys.LAST_SYNC_AT] ?: 0L }
    val preferredQuality: Flow<String> = context.detflixDataStore.data.map { it[Keys.PREFERRED_QUALITY] ?: "Auto" }
    val autoPlayNext: Flow<Boolean> = context.detflixDataStore.data.map { it[Keys.AUTO_PLAY_NEXT] ?: true }
    val subtitlesEnabled: Flow<Boolean> = context.detflixDataStore.data.map { it[Keys.SUBTITLES_ENABLED] ?: true }
    val cloudGatewayUrl: Flow<String> = context.detflixDataStore.data.map { it[Keys.CLOUD_GATEWAY_URL] ?: "" }
    val remoteCatalogUrl: Flow<String> = context.detflixDataStore.data.map { it[Keys.REMOTE_CATALOG_URL] ?: "" }

    suspend fun setSession(userId: String?, profileId: String? = null) {
        context.detflixDataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.CURRENT_USER_ID)
                prefs.remove(Keys.CURRENT_PROFILE_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
                if (profileId != null) {
                    prefs[Keys.CURRENT_PROFILE_ID] = profileId
                } else {
                    prefs.remove(Keys.CURRENT_PROFILE_ID)
                }
            }
        }
    }

    suspend fun setCurrentProfile(profileId: String?) {
        context.detflixDataStore.edit { prefs ->
            if (profileId == null) prefs.remove(Keys.CURRENT_PROFILE_ID)
            else prefs[Keys.CURRENT_PROFILE_ID] = profileId
        }
    }

    suspend fun setLastSyncAt(timestamp: Long) {
        context.detflixDataStore.edit { it[Keys.LAST_SYNC_AT] = timestamp }
    }

    suspend fun setPlayerPreferences(quality: String, autoPlayNext: Boolean, subtitlesEnabled: Boolean) {
        context.detflixDataStore.edit { prefs ->
            prefs[Keys.PREFERRED_QUALITY] = quality
            prefs[Keys.AUTO_PLAY_NEXT] = autoPlayNext
            prefs[Keys.SUBTITLES_ENABLED] = subtitlesEnabled
        }
    }

    suspend fun setCloudGatewayUrl(url: String) {
        context.detflixDataStore.edit { it[Keys.CLOUD_GATEWAY_URL] = url.trim() }
    }

    suspend fun setRemoteCatalogUrl(url: String) {
        context.detflixDataStore.edit { it[Keys.REMOTE_CATALOG_URL] = url.trim() }
    }
}
