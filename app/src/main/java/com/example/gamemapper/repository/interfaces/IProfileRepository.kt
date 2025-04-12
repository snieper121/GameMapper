package com.example.gamemapper.repository.interfaces

import com.example.gamemapper.GameProfile

/**
 * Интерфейс для работы с профилями игр
 */
interface IProfileRepository {
    /**
     * Получить все профили
     */
    fun getAllProfiles(): List<GameProfile>

    /**
     * Получить профиль по ID
     */
    fun getProfileById(profileId: String): GameProfile?

    /**
     * Получить профиль для конкретного пакета приложения
     */
    fun getProfileForPackage(packageName: String): GameProfile?

    /**
     * Сохранить профиль
     */
    fun saveProfile(profile: GameProfile)

    /**
     * Удалить профиль
     */
    fun deleteProfile(profileId: String): Boolean

    /**
     * Создать новый профиль
     */
    fun createProfile(name: String): GameProfile

    /**
     * Дублировать существующий профиль
     */
    fun duplicateProfile(profileId: String, newName: String): GameProfile?
}
