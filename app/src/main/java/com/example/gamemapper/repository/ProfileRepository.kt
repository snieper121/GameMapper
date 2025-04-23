package com.example.gamemapper.repository

import android.content.Context
import android.util.Log
import com.example.gamemapper.GameProfile
import com.example.gamemapper.PersistenceManager
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.repository.interfaces.IProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Реализация репозитория для работы с профилями
 */
class ProfileRepository(private val context: Context) : IProfileRepository {

    private val profiles = mutableListOf<GameProfile>()
    private val errorHandler by lazy { 
        try {
            AppModule.getErrorHandler()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Ошибка при получении ErrorHandler", e)
            null
        }
    }

    init {
        // Загружаем профили при инициализации
        loadProfiles()
    }

    private fun loadProfiles() {
        try {
            profiles.clear()
            profiles.addAll(PersistenceManager.loadProfiles())
        } catch (e: Exception) {
            handleError(e, "Error loading profiles")
        }
    }

    private fun handleError(e: Exception, message: String) {
        Log.e("ProfileRepository", "$message: ${e.message}", e)
        errorHandler?.handleError(e, message)
    }

    override fun getAllProfiles(): List<GameProfile> {
        return profiles.toList()
    }

    override fun getProfileById(profileId: String): GameProfile? {
        return profiles.find { it.id == profileId }
    }

    override fun getProfileForPackage(packageName: String): GameProfile? {
        return profiles.find { it.packageName == packageName }
    }

    override fun saveProfile(profile: GameProfile) {
        try {
            val existingIndex = profiles.indexOfFirst { it.id == profile.id }
            if (existingIndex >= 0) {
                profiles[existingIndex] = profile
            } else {
                profiles.add(profile)
            }
            PersistenceManager.saveProfiles(profiles)
        } catch (e: Exception) {
            handleError(e, "Не удалось сохранить профиль")
            throw e
        }
    }

    override fun deleteProfile(profileId: String): Boolean {
        return try {
            val removed = profiles.removeIf { it.id == profileId }
            if (removed) {
                PersistenceManager.saveProfiles(profiles)
            }
            removed
        } catch (e: Exception) {
            handleError(e, "Не удалось удалить профиль")
            false
        }
    }

    override fun createProfile(name: String): GameProfile {
        try {
            val newProfile = GameProfile(
                id = UUID.randomUUID().toString(),
                name = name
            )
            profiles.add(newProfile)
            PersistenceManager.saveProfiles(profiles)
            return newProfile
        } catch (e: Exception) {
            handleError(e, "Не удалось создать профиль")
            throw e
        }
    }

    override fun duplicateProfile(profileId: String, newName: String): GameProfile? {
        try {
            val originalProfile = getProfileById(profileId) ?: return null

            // Создаем копию профиля
            val duplicatedProfile = originalProfile.copyWithMappings(
                id = UUID.randomUUID().toString(),
                name = newName,
                isActive = false
            )

            profiles.add(duplicatedProfile)
            PersistenceManager.saveProfiles(profiles)
            return duplicatedProfile
        } catch (e: Exception) {
            handleError(e, "Не удалось дублировать профиль")
            return null
        }
    }
}
