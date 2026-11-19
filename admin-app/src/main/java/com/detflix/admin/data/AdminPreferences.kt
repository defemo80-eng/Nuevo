package com.detflix.admin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.adminDataStore: DataStore<Preferences> by preferencesDataStore(name = "detflix_admin_prefs")

data class AdminSettings(
    val monthlyPriceUsd: Double = 9.99,
    val defaultDurationDays: Int = 30,
    val platformName: String = "DETFLIX",
    val supportChannel: String = "Soporte oficial DETFLIX",
    val requirePin: Boolean = true,
    val adminPin: String = "2013"
)

class AdminPreferences(private val context: Context) {
    private object Keys {
        val MONTHLY_PRICE = doublePreferencesKey("monthly_price_usd")
        val DURATION_DAYS = intPreferencesKey("default_duration_days")
        val PLATFORM_NAME = stringPreferencesKey("platform_name")
        val SUPPORT_CHANNEL = stringPreferencesKey("support_channel")
        val REQUIRE_PIN = booleanPreferencesKey("require_pin")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
    }

    val settingsFlow: Flow<AdminSettings> = context.adminDataStore.data.map { prefs ->
        AdminSettings(
            monthlyPriceUsd = prefs[Keys.MONTHLY_PRICE] ?: 9.99,
            defaultDurationDays = prefs[Keys.DURATION_DAYS] ?: 30,
            platformName = prefs[Keys.PLATFORM_NAME] ?: "DETFLIX",
            supportChannel = prefs[Keys.SUPPORT_CHANNEL] ?: "Soporte oficial DETFLIX",
            requirePin = prefs[Keys.REQUIRE_PIN] ?: true,
            adminPin = prefs[Keys.ADMIN_PIN] ?: "2013"
        )
    }

    suspend fun saveSettings(settings: AdminSettings) {
        context.adminDataStore.edit { prefs ->
            prefs[Keys.MONTHLY_PRICE] = settings.monthlyPriceUsd
            prefs[Keys.DURATION_DAYS] = settings.defaultDurationDays
            prefs[Keys.PLATFORM_NAME] = settings.platformName.ifBlank { "DETFLIX" }
            prefs[Keys.SUPPORT_CHANNEL] = settings.supportChannel.ifBlank { "Soporte oficial DETFLIX" }
            prefs[Keys.REQUIRE_PIN] = settings.requirePin
            prefs[Keys.ADMIN_PIN] = settings.adminPin.ifBlank { "2013" }
        }
    }
}
