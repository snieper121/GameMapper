package com.example.gamemapper.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var settingsRepository: ISettingsRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
        settingsRepository.clearAll()
    }
    
    @Test
    fun testBooleanSettings() {
        val key = "test_boolean"
        val value = true
        
        // Проверяем значение по умолчанию
        assertEquals(false, settingsRepository.getBoolean(key, false))
        
        // Сохраняем значение
        settingsRepository.saveBoolean(key, value)
        
        // Проверяем сохраненное значение
        assertEquals(value, settingsRepository.getBoolean(key, false))
    }
    
    @Test
    fun testIntSettings() {
        val key = "test_int"
        val value = 42
        
        // Проверяем значение по умолчанию
        assertEquals(0, settingsRepository.getInt(key, 0))
        
        // Сохраняем значение
        settingsRepository.saveInt(key, value)
        
        // Проверяем сохраненное значение
        assertEquals(value, settingsRepository.getInt(key, 0))
    }
    
    @Test
    fun testFloatSettings() {
        val key = "test_float"
        val value = 3.14f
        
        // Проверяем значение по умолчанию
        assertEquals(0f, settingsRepository.getFloat(key, 0f))
        
        // Сохраняем значение
        settingsRepository.saveFloat(key, value)
        
        // Проверяем сохраненное значение
        assertEquals(value, settingsRepository.getFloat(key, 0f))
    }
    
    @Test
    fun testStringSettings() {
        val key = "test_string"
        val value = "Hello, World!"
        
        // Проверяем значение по умолчанию
        assertEquals("", settingsRepository.getString(key, ""))
        
        // Сохраняем значение
        settingsRepository.saveString(key, value)
        
        // Проверяем сохраненное значение
        assertEquals(value, settingsRepository.getString(key, ""))
    }
    
    @Test
    fun testLastActiveProfileId() {
        val profileId = "test_profile_id"
        
        // Проверяем значение по умолчанию
        assertNull(settingsRepository.getLastActiveProfileId())
        
        // Сохраняем значение
        settingsRepository.saveLastActiveProfileId(profileId)
        
        // Проверяем сохраненное значение
        assertEquals(profileId, settingsRepository.getLastActiveProfileId())
    }
    
    @Test
    fun testClearAll() {
        // Сохраняем несколько настроек
        settingsRepository.saveBoolean("test_boolean", true)
        settingsRepository.saveInt("test_int", 42)
        settingsRepository.saveFloat("test_float", 3.14f)
        settingsRepository.saveString("test_string", "Hello, World!")
        settingsRepository.saveLastActiveProfileId("test_profile_id")
        
        // Очищаем все настройки
        settingsRepository.clearAll()
        
        // Проверяем, что все настройки сброшены
        assertEquals(false, settingsRepository.getBoolean("test_boolean", false))
        assertEquals(0, settingsRepository.getInt("test_int", 0))
        assertEquals(0f, settingsRepository.getFloat("test_float", 0f))
        assertEquals("", settingsRepository.getString("test_string", ""))
        assertNull(settingsRepository.getLastActiveProfileId())
    }
}