package com.example.gamemapper.helpers

import android.content.Context
import android.util.Log
import android.view.View
import com.example.gamemapper.di.AppModule

/**
 * Оптимизированный менеджер оверлеев с управлением памятью
 */
class MemoryOptimizedOverlayManager(
    private val context: Context,
    private val overlayManager: OverlayManager
) {

    companion object {
        private const val TAG = "MemoryOptimizedOverlayManager"
        private const val MAX_ACTIVE_OVERLAYS = 20
    }

    private val errorHandler = AppModule.getErrorHandler()

    // Хранит информацию о всех маппингах
    private val allMappings = mutableMapOf<Int, Pair<Float, Float>>()

    // Хранит информацию о видимых в данный момент оверлеях
    private val visibleOverlays = mutableSetOf<Int>()

    /**
     * Устанавливает все маппинги
     */
    fun setMappings(mappings: Map<Int, Pair<Float, Float>>) {
        allMappings.clear()
        allMappings.putAll(mappings)

        // Сбрасываем видимые оверлеи
        overlayManager.removeAllOverlayButtons()
        visibleOverlays.clear()

        // Показываем только необходимое количество оверлеев
        showPriorityOverlays()
    }

    /**
     * Добавляет маппинг
     */
    fun addMapping(keyCode: Int, x: Float, y: Float) {
        allMappings[keyCode] = Pair(x, y)

        // Если оверлеев меньше максимума, показываем новый
        if (visibleOverlays.size < MAX_ACTIVE_OVERLAYS) {
            showOverlay(keyCode)
        }
    }

    /**
     * Удаляет маппинг
     */
    fun removeMapping(keyCode: Int) {
        allMappings.remove(keyCode)

        // Удаляем оверлей, если он был видимым
        if (visibleOverlays.contains(keyCode)) {
            overlayManager.removeOverlayButton(keyCode)
            visibleOverlays.remove(keyCode)

            // Показываем следующий приоритетный оверлей
            showNextPriorityOverlay()
        }
    }

    /**
     * Обновляет позицию маппинга
     */
    fun updateMappingPosition(keyCode: Int, x: Float, y: Float) {
        allMappings[keyCode] = Pair(x, y)

        // Если оверлей видимый, обновляем его позицию
        if (visibleOverlays.contains(keyCode)) {
            overlayManager.updateOverlayButtonPosition(keyCode, x, y)
        }
    }

    /**
     * Показывает оверлей для указанного keyCode
     */
    private fun showOverlay(keyCode: Int) {
        val position = allMappings[keyCode] ?: return

        val success = overlayManager.addOverlayButton(
            keyCode,
            position.first,
            position.second
        ) { pressedKeyCode ->
            // Обработчик нажатия на кнопку
            val inputHandler = AppModule.getInputEventHandler()
            inputHandler.injectKeyPress(pressedKeyCode)
        }

        if (success) {
            visibleOverlays.add(keyCode)
        }
    }

    /**
     * Показывает оверлеи с наивысшим приоритетом
     */
    private fun showPriorityOverlays() {
        // Очищаем все видимые оверлеи
        overlayManager.removeAllOverlayButtons()
        visibleOverlays.clear()

        // Показываем только MAX_ACTIVE_OVERLAYS оверлеев
        allMappings.keys.take(MAX_ACTIVE_OVERLAYS).forEach { keyCode ->
            showOverlay(keyCode)
        }
    }

    /**
     * Показывает следующий приоритетный оверлей
     */
    private fun showNextPriorityOverlay() {
        // Находим первый keyCode, который не отображается
        val nextKeyCode = allMappings.keys.firstOrNull {
            !visibleOverlays.contains(it)
        } ?: return

        showOverlay(nextKeyCode)
    }

    /**
     * Очищает все оверлеи и маппинги
     */
    fun clearAll() {
        overlayManager.removeAllOverlayButtons()
        visibleOverlays.clear()
        allMappings.clear()
    }

    /**
     * Временно скрывает все оверлеи
     */
    fun hideAllOverlays() {
        overlayManager.removeAllOverlayButtons()
        visibleOverlays.clear()
    }

    /**
     * Восстанавливает видимость оверлеев
     */
    fun restoreOverlays() {
        showPriorityOverlays()
    }
}
