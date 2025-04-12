package com.example.gamemapper.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.repository.interfaces.ISettingsRepository

/**
 * Реализация репозитория для работы с настройками
 */
class SettingsRepository(context: Context) : ISettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("GameMapperSettings", Context.MODE_PRIVATE)
    private val errorHandler = AppModule.getErrorHandler()

    companion object {
        private const val LAST_ACTIVE_PROFILE_KEY = "last_active_profile"
        private const val BLOCK_TOUCH_EVENTS_KEY = "block_touch_events"
        private const val EXTERNAL_DEVICES_ONLY_KEY = "external_devices_only"
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            errorHandler.logError(e, "Error getting boolean setting: $key")
            defaultValue
        }
    }

    override fun saveBoolean(key: String, value: Boolean) {
        try {
            prefs.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            errorHandler.logError(e, "Error saving boolean setting: $key")
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return try {
            prefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            errorHandler.logError(e, "Error getting int setting: $key")
            defaultValue
        }
    }

    override fun saveInt(key: String, value: Int) {
        try {
            prefs.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            errorHandler.logError(e, "Error saving int setting: $key")
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            prefs.getFloat(key, defaultValue)
        } catch (e: Exception) {
            errorHandler.logError(e, "Error getting float setting: $key")
            defaultValue
        }
    }

    override fun saveFloat(key: String, value: Float) {
        try {
            prefs.edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            errorHandler.logError(e, "Error saving float setting: $key")
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return try {
            prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            errorHandler.logError(e, "Error getting string setting: $key")
            defaultValue
        }
    }

    override fun saveString(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            errorHandler.logError(e, "Error saving string setting: $key")
        }
    }

    override fun getLastActiveProfileId(): String? {
        val profileId = getString(LAST_ACTIVE_PROFILE_KEY, "")
        return if (profileId.isEmpty()) null else profileId
    }

    override fun saveLastActiveProfileId(profileId: String) {
        saveString(LAST_ACTIVE_PROFILE_KEY, profileId)
    }

    override fun isBlockTouchEventsEnabled(): Boolean {
        return getBoolean(BLOCK_TOUCH_EVENTS_KEY, true)
    }

    override fun setBlockTouchEvents(enabled: Boolean) {
        saveBoolean(BLOCK_TOUCH_EVENTS_KEY, enabled)
    }

    override fun isExternalDevicesOnlyEnabled(): Boolean {
        return getBoolean(EXTERNAL_DEVICES_ONLY_KEY, true)
    }

    override fun setExternalDevicesOnly(enabled: Boolean) {
        saveBoolean(EXTERNAL_DEVICES_ONLY_KEY, enabled)
    }

    override fun clearAll() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            errorHandler.logError(e, "Error clearing all settings")
        }
    }
}

