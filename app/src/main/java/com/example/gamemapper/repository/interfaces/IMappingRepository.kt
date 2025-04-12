package com.example.gamemapper.repository.interfaces

import com.example.gamemapper.GestureMapping

/**
 * Интерфейс для работы с маппингами кнопок и жестов
 */
interface IMappingRepository {
    /**
     * Получить все маппинги клавиш
     */
    fun getKeyMappings(): Map<Int, Pair<Float, Float>>

    /**
     * Обновить маппинги клавиш
     */
    fun updateKeyMappings(mappings: Map<Int, Pair<Float, Float>>)

    /**
     * Добавить маппинг клавиши
     */
    fun addKeyMapping(keyCode: Int, x: Float, y: Float)

    /**
     * Удалить маппинг клавиши
     */
    fun removeKeyMapping(keyCode: Int)

    /**
     * Получить все маппинги жестов
     */
    fun getGestureMappings(): List<GestureMapping>

    /**
     * Добавить маппинг жеста
     */
    fun addGestureMapping(gestureMapping: GestureMapping)

    /**
     * Удалить маппинг жеста
     */
    fun removeGestureMapping(gestureId: String)

    /**
     * Сохранить все маппинги
     */
    fun saveMappings()

    /**
     * Загрузить все маппинги
     */
    fun loadMappings()
}
