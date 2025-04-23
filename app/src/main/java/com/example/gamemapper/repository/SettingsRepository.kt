package com.example.gamemapper.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import com.example.gamemapper.ButtonSettingsDialog.ButtonSettings
import com.example.gamemapper.CustomOverlayButton
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Реализация репозитория для работы с настройками
 */
class SettingsRepository(context: Context) : ISettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("GameMapperSettings", Context.MODE_PRIVATE)
    private val errorHandler by lazy { 
        try {
            AppModule.getErrorHandler()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении ErrorHandler", e)
            null
        }
    }
    private val gson: Gson

    init {
        // Настраиваем Gson для безопасной сериализации/десериализации
        gson = GsonBuilder()
            .serializeNulls()
            .create()
    }

    companion object {
        private const val TAG = "SettingsRepository"
        private const val LAST_ACTIVE_PROFILE_KEY = "last_active_profile"
        private const val BLOCK_TOUCH_EVENTS_KEY = "block_touch_events"
        private const val EXTERNAL_DEVICES_ONLY_KEY = "external_devices_only"
        private const val BUTTON_SETTINGS_PREFIX = "button_settings_"
    }

    private fun logError(e: Exception, message: String) {
        Log.e(TAG, "$message: ${e.message}", e)
        errorHandler?.logError(e, message)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            logError(e, "Error getting boolean setting: $key")
            defaultValue
        }
    }

    override fun saveBoolean(key: String, value: Boolean) {
        try {
            prefs.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            logError(e, "Error saving boolean setting: $key")
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return try {
            prefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            logError(e, "Error getting int setting: $key")
            defaultValue
        }
    }

    override fun saveInt(key: String, value: Int) {
        try {
            prefs.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            logError(e, "Error saving int setting: $key")
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            prefs.getFloat(key, defaultValue)
        } catch (e: Exception) {
            logError(e, "Error getting float setting: $key")
            defaultValue
        }
    }

    override fun saveFloat(key: String, value: Float) {
        try {
            prefs.edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            logError(e, "Error saving float setting: $key")
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return try {
            prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logError(e, "Error getting string setting: $key")
            defaultValue
        }
    }

    override fun saveString(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            logError(e, "Error saving string setting: $key")
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
    
    override fun getButtonSettings(keyCode: Int): ButtonSettings {
        try {
            val key = BUTTON_SETTINGS_PREFIX + keyCode
            val json = prefs.getString(key, null) ?: return ButtonSettings()
            return gson.fromJson(json, ButtonSettings::class.java) ?: ButtonSettings()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting button settings for keyCode $keyCode", e)
            return ButtonSettings()
        }
    }
    
    override fun saveButtonSettings(keyCode: Int, settings: ButtonSettings) {
        try {
            if (settings.buttonText.isBlank()) {
                settings.buttonText = KeyEvent.keyCodeToString(keyCode)
            }
            val json = gson.toJson(settings)
            val key = BUTTON_SETTINGS_PREFIX + keyCode
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving button settings for keyCode $keyCode", e)
        }
    }
    
    override fun deleteButtonSettings(keyCode: Int) {
        try {
            val key = BUTTON_SETTINGS_PREFIX + keyCode
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting button settings for keyCode $keyCode", e)
        }
    }

    override fun clearAll() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            logError(e, "Error clearing all settings")
        }
    }
}

