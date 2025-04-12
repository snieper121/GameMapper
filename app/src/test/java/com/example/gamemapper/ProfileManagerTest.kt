package com.example.gamemapper

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ProfileManagerTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Мокируем PersistenceManager для тестирования
        val mockPersistenceManager = mock(PersistenceManager::class.java)
        `when`(mockPersistenceManager.loadProfiles()).thenReturn(emptyList())
        
        // Инициализация ProfileManager
        ProfileManager.init(context, mockPersistenceManager)
    }
    
    @After
    fun tearDown() {
        // Очищаем профили после каждого теста
        ProfileManager.clearAllProfiles()
    }
    
    @Test
    fun testCreateNewProfile() {
        val profileName = "Test Profile"
        val profile = ProfileManager.createNewProfile(profileName)
        
        assertEquals(profileName, profile.name)
        assertTrue(ProfileManager.getAllProfiles().contains(profile))
    }
    
    @Test
    fun testGetProfileForPackage() {
        val packageName = "com.example.testapp"
        val profile = ProfileManager.createNewProfile("Package Test")
        profile.packageName = packageName
        ProfileManager.saveProfile(profile)
        
        val retrievedProfile = ProfileManager.getProfileForPackage(packageName)
        assertNotNull(retrievedProfile)
        assertEquals(profile.id, retrievedProfile?.id)
    }
    
    @Test
    fun testDeleteProfile() {
        val profile = ProfileManager.createNewProfile("Delete Test")
        val profileId = profile.id
        
        assertTrue(ProfileManager.getAllProfiles().any { it.id == profileId })
        
        ProfileManager.deleteProfile(profileId)
        
        assertFalse(ProfileManager.getAllProfiles().any { it.id == profileId })
        assertNull(ProfileManager.getProfile(profileId))
    }
    
    @Test
    fun testDuplicateProfile() {
        val originalName = "Original Profile"
        val duplicateName = "Duplicate Profile"
        
        val originalProfile = ProfileManager.createNewProfile(originalName)
        originalProfile.keyMappings[KeyEvent.KEYCODE_A] = Pair(100f, 200f)
        originalProfile.keyMappings[KeyEvent.KEYCODE_B] = Pair(300f, 400f)
        ProfileManager.saveProfile(originalProfile)
        
        val duplicatedProfile = ProfileManager.duplicateProfile(originalProfile.id, duplicateName)
        
        assertNotNull(duplicatedProfile)
        // Добавляем проверку на null перед обращением к свойствам
        duplicatedProfile?.let {
            assertEquals(duplicateName, it.name)
            assertEquals(originalProfile.keyMappings.size, it.keyMappings.size)
            assertEquals(
                originalProfile.keyMappings[KeyEvent.KEYCODE_A],
                it.keyMappings[KeyEvent.KEYCODE_A]
            )
        }
    }
}