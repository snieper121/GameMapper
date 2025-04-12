package com.example.gamemapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class PersistenceManagerTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PersistenceManager.init(context)
        
        // Очищаем тестовые файлы перед каждым тестом
        val profilesFile = File(context.filesDir, "profiles.json")
        if (profilesFile.exists()) {
            profilesFile.delete()
        }
    }
    
    @Test
    fun testSaveAndLoadProfiles() {
        // Создаем тестовые профили
        val profile1 = GameProfile(name = "Test Profile 1")
        val profile2 = GameProfile(name = "Test Profile 2")
        
        // Сохраняем профили
        PersistenceManager.saveProfiles(listOf(profile1, profile2))
        
        // Загружаем профили
        val loadedProfiles = PersistenceManager.loadProfiles()
        
        // Проверяем, что профили загружены корректно
        assertEquals(2, loadedProfiles.size)
        assertTrue(loadedProfiles.any { it.name == "Test Profile 1" })
        assertTrue(loadedProfiles.any { it.name == "Test Profile 2" })
    }
    
    @Test
    fun testSaveAndLoadEmptyProfiles() {
        // Сохраняем пустой список профилей
        PersistenceManager.saveProfiles(emptyList())
        
        // Загружаем профили
        val loadedProfiles = PersistenceManager.loadProfiles()
        
        // Проверяем, что загружен пустой список
        assertTrue(loadedProfiles.isEmpty())
    }
    
    @Test
    fun testSaveAndLoadProfilesWithMappings() {
        // Создаем профиль с маппингами
        val profile = GameProfile(name = "Mapping Test")
        profile.keyMappings[android.view.KeyEvent.KEYCODE_A] = Pair(100f, 200f)
        profile.keyMappings[android.view.KeyEvent.KEYCODE_B] = Pair(300f, 400f)
        
        // Добавляем жест (убедитесь, что все необходимые параметры указаны)
        val gesture = GestureMapping(
            gestureType = GestureType.TAP,
            keyCode = android.view.KeyEvent.KEYCODE_X,
            startX = 150f,
            startY = 250f,
            // Добавляем все обязательные параметры
            endX = 150f,
            endY = 250f,
            duration = 0
        )
        
        profile.gestureMappings.add(gesture)
        
        // Сохраняем профиль
        PersistenceManager.saveProfiles(listOf(profile))
        
        // Загружаем профили
        val loadedProfiles = PersistenceManager.loadProfiles()
        
        // Проверяем, что профиль загружен корректно
        assertEquals(1, loadedProfiles.size)
        val loadedProfile = loadedProfiles.first()
        
        // Проверяем маппинги
        assertEquals(2, loadedProfile.keyMappings.size)
        assertEquals(Pair(100f, 200f), loadedProfile.keyMappings[android.view.KeyEvent.KEYCODE_A])
        assertEquals(Pair(300f, 400f), loadedProfile.keyMappings[android.view.KeyEvent.KEYCODE_B])
        
        // Проверяем жесты
        assertEquals(1, loadedProfile.gestureMappings.size)
        val loadedGesture = loadedProfile.gestureMappings.first()
        assertEquals(GestureType.TAP, loadedGesture.gestureType)
        assertEquals(android.view.KeyEvent.KEYCODE_X, loadedGesture.keyCode)
        assertEquals(150f, loadedGesture.startX)
        assertEquals(250f, loadedGesture.startY)
    }
}