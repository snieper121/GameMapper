package com.example.gamemapper.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gamemapper.GameProfile
import com.example.gamemapper.PersistenceManager
import com.example.gamemapper.repository.interfaces.IProfileRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ProfileRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var profileRepository: IProfileRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Мокируем PersistenceManager для тестирования
        val mockPersistenceManager = mock(PersistenceManager::class.java)
        `when`(mockPersistenceManager.loadProfiles()).thenReturn(emptyList())
        
        // Инициализация репозитория
        profileRepository = ProfileRepository(context)
    }
    
    @Test
    fun testCreateProfile() {
        val profileName = "Test Profile"
        val profile = profileRepository.createProfile(profileName)
        
        assertEquals(profileName, profile.name)
        assertTrue(profileRepository.getAllProfiles().contains(profile))
    }
    
    @Test
    fun testGetProfileForPackage() {
        val packageName = "com.example.testapp"
        val profile = profileRepository.createProfile("Package Test")
        profile.packageName = packageName
        profileRepository.saveProfile(profile)
        
        val retrievedProfile = profileRepository.getProfileForPackage(packageName)
        assertNotNull(retrievedProfile)
        assertEquals(profile.id, retrievedProfile?.id)
    }
    
    @Test
    fun testDeleteProfile() {
        val profile = profileRepository.createProfile("Delete Test")
        val profileId = profile.id
        
        assertTrue(profileRepository.getAllProfiles().any { it.id == profileId })
        
        profileRepository.deleteProfile(profileId)
        
        assertFalse(profileRepository.getAllProfiles().any { it.id == profileId })
        assertNull(profileRepository.getProfileById(profileId))
    }
    
    @Test
    fun testDuplicateProfile() {
        val originalName = "Original Profile"
        val duplicateName = "Duplicate Profile"
        
        val originalProfile = profileRepository.createProfile(originalName)
        originalProfile.keyMappings[android.view.KeyEvent.KEYCODE_A] = Pair(100f, 200f)
        originalProfile.keyMappings[android.view.KeyEvent.KEYCODE_B] = Pair(300f, 400f)
        profileRepository.saveProfile(originalProfile)
        
        val duplicatedProfile = profileRepository.duplicateProfile(originalProfile.id, duplicateName)
        
        assertNotNull(duplicatedProfile)
        duplicatedProfile?.let {
            assertEquals(duplicateName, it.name)
            assertEquals(originalProfile.keyMappings.size, it.keyMappings.size)
            assertEquals(
                originalProfile.keyMappings[android.view.KeyEvent.KEYCODE_A],
                it.keyMappings[android.view.KeyEvent.KEYCODE_A]
            )
        }
    }
}