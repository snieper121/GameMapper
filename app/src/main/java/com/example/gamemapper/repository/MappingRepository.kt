package com.example.gamemapper.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.gamemapper.GestureMapping
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.repository.interfaces.IMappingRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация репозитория для работы с маппингами
 */
class MappingRepository(private val context: Context) : IMappingRepository {

    private val keyMappings = mutableMapOf<Int, Pair<Float, Float>>()
    private val gestureMappings = mutableListOf<GestureMapping>()
    private val prefs: SharedPreferences = context.getSharedPreferences("MappingPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val errorHandler = AppModule.getErrorHandler()

    companion object {
        private const val TAG = "MappingRepository"
        private const val KEY_MAPPINGS_KEY = "key_mappings"
        private const val GESTURE_MAPPINGS_KEY = "gesture_mappings"
    }

    init {
        loadMappings()
    }

    override fun getKeyMappings(): Map<Int, Pair<Float, Float>> {
        return keyMappings.toMap()
    }

    override fun updateKeyMappings(mappings: Map<Int, Pair<Float, Float>>) {
        try {
            keyMappings.clear()
            keyMappings.putAll(mappings)
            saveMappings()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось обновить маппинги клавиш")
        }
    }

    override fun addKeyMapping(keyCode: Int, x: Float, y: Float) {
        try {
            keyMappings[keyCode] = Pair(x, y)
            saveMappings()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось добавить маппинг клавиши")
        }
    }

    override fun removeKeyMapping(keyCode: Int) {
        try {
            keyMappings.remove(keyCode)
            saveMappings()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось удалить маппинг клавиши")
        }
    }

    override fun getGestureMappings(): List<GestureMapping> {
        return gestureMappings.toList()
    }

    override fun addGestureMapping(gestureMapping: GestureMapping) {
        try {
            val existingIndex = gestureMappings.indexOfFirst { it.id == gestureMapping.id }
            if (existingIndex >= 0) {
                gestureMappings[existingIndex] = gestureMapping
            } else {
                gestureMappings.add(gestureMapping)
            }
            saveMappings()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось добавить маппинг жеста")
        }
    }

    override fun removeGestureMapping(gestureId: String) {
        try {
            gestureMappings.removeIf { it.id == gestureId }
            saveMappings()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось удалить маппинг жеста")
        }
    }

    override fun saveMappings() {
        try {
            val keyMappingsJson = gson.toJson(keyMappings)
            val gestureMappingsJson = gson.toJson(gestureMappings)

            // Используем батчинг для операций с SharedPreferences
            prefs.edit()
                .putString(KEY_MAPPINGS_KEY, keyMappingsJson)
                .putString(GESTURE_MAPPINGS_KEY, gestureMappingsJson)
                .apply()

            Log.d(TAG, "Mappings saved successfully")
        } catch (e: Exception) {
            errorHandler.logError(e, "Error saving mappings: ${e.message}")
        }
    }

    override fun loadMappings() {
        try {
            val keyMappingsJson = prefs.getString(KEY_MAPPINGS_KEY, null)
            val gestureMappingsJson = prefs.getString(GESTURE_MAPPINGS_KEY, null)

            if (keyMappingsJson != null) {
                val type = object : TypeToken<Map<Int, Pair<Float, Float>>>() {}.type
                val loadedMappings: Map<Int, Pair<Float, Float>> = gson.fromJson(keyMappingsJson, type)
                keyMappings.clear()
                keyMappings.putAll(loadedMappings)
            }

            if (gestureMappingsJson != null) {
                val type = object : TypeToken<List<GestureMapping>>() {}.type
                val loadedGestures: List<GestureMapping> = gson.fromJson(gestureMappingsJson, type)
                gestureMappings.clear()
                gestureMappings.addAll(loadedGestures)
            }

            Log.d(TAG, "Mappings loaded successfully")
        } catch (e: Exception) {
            errorHandler.logError(e, "Error loading mappings: ${e.message}")
        }
    }
}
