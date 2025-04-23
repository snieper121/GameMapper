package com.example.gamemapper.repository.interfaces

import com.example.gamemapper.ButtonSettingsDialog.ButtonSettings

/**
 * Интерфейс для работы с настройками приложения
 */
interface ISettingsRepository {
    /**
     * Получить значение настройки типа Boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    /**
     * Сохранить значение настройки типа Boolean
     */
    fun saveBoolean(key: String, value: Boolean)

    /**
     * Получить значение настройки типа Int
     */
    fun getInt(key: String, defaultValue: Int): Int

    /**
     * Сохранить значение настройки типа Int
     */
    fun saveInt(key: String, value: Int)

    /**
     * Получить значение настройки типа Float
     */
    fun getFloat(key: String, defaultValue: Float): Float

    /**
     * Сохранить значение настройки типа Float
     */
    fun saveFloat(key: String, value: Float)

    /**
     * Получить значение настройки типа String
     */
    fun getString(key: String, defaultValue: String): String

    /**
     * Сохранить значение настройки типа String
     */
    fun saveString(key: String, value: String)

    /**
     * Получить последний активный профиль
     */
    fun getLastActiveProfileId(): String?

    /**
     * Сохранить последний активный профиль
     */
    fun saveLastActiveProfileId(profileId: String)

    /**
     * Проверить, включена ли блокировка сенсорных событий
     */
    fun isBlockTouchEventsEnabled(): Boolean

    /**
     * Установить блокировку сенсорных событий
     */
    fun setBlockTouchEvents(enabled: Boolean)

    /**
     * Проверить, включен ли режим только внешних устройств
     */
    fun isExternalDevicesOnlyEnabled(): Boolean

    /**
     * Установить режим только внешних устройств
     */
    fun setExternalDevicesOnly(enabled: Boolean)

    /**
     * Получить настройки кнопки для указанного keyCode
     */
    fun getButtonSettings(keyCode: Int): ButtonSettings

    /**
     * Сохранить настройки кнопки для указанного keyCode
     */
    fun saveButtonSettings(keyCode: Int, settings: ButtonSettings)

    /**
     * Удалить настройки кнопки для указанного keyCode
     */
    fun deleteButtonSettings(keyCode: Int)

    /**
     * Очистить все настройки
     */
    fun clearAll()
}
