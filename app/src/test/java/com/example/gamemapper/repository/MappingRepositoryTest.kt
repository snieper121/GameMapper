package com.example.gamemapper.repository

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.gamemapper.GestureMapping
import com.example.gamemapper.GestureType
import com.example.gamemapper.repository.interfaces.IMappingRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MappingRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var mappingRepository: IMappingRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mappingRepository = MappingRepository(context)
        
        // Очищаем все маппинги перед каждым тестом
        val allMappings = mappingRepository.getKeyMappings().keys.toList()
        for (keyCode in allMappings) {
            mappingRepository.removeKeyMapping(keyCode)
        }
        
        val allGestures = mappingRepository.getGestureMappings().map { it.id }
        for (gestureId in allGestures) {
            mappingRepository.removeGestureMapping(gestureId)
        }
    }
    
    @Test
    fun testAddAndGetKeyMapping() {
        // Добавляем маппинг
        mappingRepository.addKeyMapping(KeyEvent.KEYCODE_A, 100f, 200f)
        
        // Проверяем, что маппинг добавлен
        val mappings = mappingRepository.getKeyMappings()
        assertEquals(1, mappings.size)
        assertTrue(mappings.containsKey(KeyEvent.KEYCODE_A))
        assertEquals(Pair(100f, 200f), mappings[KeyEvent.KEYCODE_A])
    }
    
    @Test
    fun testRemoveKeyMapping() {
        // Добавляем маппинг
        mappingRepository.addKeyMapping(KeyEvent.KEYCODE_A, 100f, 200f)
        
        // Проверяем, что маппинг добавлен
        var mappings = mappingRepository.getKeyMappings()
        assertEquals(1, mappings.size)
        
        // Удаляем маппинг
        mappingRepository.removeKeyMapping(KeyEvent.KEYCODE_A)
        
        // Проверяем, что маппинг удален
        mappings = mappingRepository.getKeyMappings()
        assertEquals(0, mappings.size)
    }
    
    @Test
    fun testUpdateKeyMappings() {
        // Добавляем несколько маппингов
        val initialMappings = mapOf(
            KeyEvent.KEYCODE_A to Pair(100f, 200f),
            KeyEvent.KEYCODE_B to Pair(300f, 400f)
        )
        
        mappingRepository.updateKeyMappings(initialMappings)
        
        // Проверяем, что маппинги добавлены
        var mappings = mappingRepository.getKeyMappings()
        assertEquals(2, mappings.size)
        
        // Обновляем маппинги
        val updatedMappings = mapOf(
            KeyEvent.KEYCODE_C to Pair(500f, 600f),
            KeyEvent.KEYCODE_D to Pair(700f, 800f)
        )
        
        mappingRepository.updateKeyMappings(updatedMappings)
        
        // Проверяем, что маппинги обновлены
        mappings = mappingRepository.getKeyMappings()
        assertEquals(2, mappings.size)
        assertTrue(mappings.containsKey(KeyEvent.KEYCODE_C))
        assertTrue(mappings.containsKey(KeyEvent.KEYCODE_D))
        assertFalse(mappings.containsKey(KeyEvent.KEYCODE_A))
        assertFalse(mappings.containsKey(KeyEvent.KEYCODE_B))
    }
    
    @Test
    fun testAddAndGetGestureMapping() {
        // Создаем жест
        val gesture = GestureMapping(
            gestureType = GestureType.TAP,
            keyCode = KeyEvent.KEYCODE_X,
            startX = 150f,
            startY = 250f,
            endX = 150f,
            endY = 250f,
            duration = 0
        )
        
        // Добавляем жест
        mappingRepository.addGestureMapping(gesture)
        
        // Проверяем, что жест добавлен
        val gestures = mappingRepository.getGestureMappings()
        assertEquals(1, gestures.size)
        assertEquals(gesture.gestureType, gestures[0].gestureType)
        assertEquals(gesture.keyCode, gestures[0].keyCode)
        assertEquals(gesture.startX, gestures[0].startX)
        assertEquals(gesture.startY, gestures[0].startY)
    }
    
    @Test
    fun testRemoveGestureMapping() {
        // Создаем жест
        val gesture = GestureMapping(
            gestureType = GestureType.TAP,
            keyCode = KeyEvent.KEYCODE_X,
            startX = 150f,
            startY = 250f,
            endX = 150f,
            endY = 250f,
            duration = 0
        )
        
        // Добавляем жест
        mappingRepository.addGestureMapping(gesture)
        
        // Проверяем, что жест добавлен
        var gestures = mappingRepository.getGestureMappings()
        assertEquals(1, gestures.size)
        
        // Удаляем жест
        mappingRepository.removeGestureMapping(gesture.id)
        
        // Проверяем, что жест удален
        gestures = mappingRepository.getGestureMappings()
        assertEquals(0, gestures.size)
    }
}