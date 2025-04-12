package com.example.gamemapper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Менеджер для сохранения и загрузки данных приложения
 */
object PersistenceManager {
    private const val TAG = "PersistenceManager"
    private const val PREFS_NAME = "GameMapperPrefs"
    private const val PROFILES_KEY = "profiles"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    /**
     * Инициализация менеджера
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Сохраняет список профилей
     */
    fun saveProfiles(profiles: List<GameProfile>) {
        try {
            val json = gson.toJson(profiles)
            prefs.edit().putString(PROFILES_KEY, json).apply()
            Log.d(TAG, "Profiles saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profiles: ${e.message}", e)
        }
    }

    /**
     * Загружает список профилей
     */
    fun loadProfiles(): List<GameProfile> {
        try {
            val json = prefs.getString(PROFILES_KEY, null)
            if (json != null) {
                val type = object : TypeToken<List<GameProfile>>() {}.type
                val profiles: List<GameProfile> = gson.fromJson(json, type)
                Log.d(TAG, "Loaded ${profiles.size} profiles")
                return profiles
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profiles: ${e.message}", e)
        }
        return emptyList()
    }
}
