package com.example.gamemapper.helpers

import android.content.Context
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.core.util.forEach
import com.example.gamemapper.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

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
        private const val CLEANUP_INTERVAL_MS = 300000 // 5 минут
    }

    private val errorHandler by lazy { 
        try {
            AppModule.getErrorHandler()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении ErrorHandler", e)
            null
        }
    }
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Используем SparseArray вместо Map для повышения производительности с int ключами
    // и экономии памяти (особенно важно на Android)
    private val allMappings = SparseArray<Pair<Float, Float>>()
    
    // Для хранения приоритетных ключей
    private val keysByPriority = ArrayList<Int>(MAX_ACTIVE_OVERLAYS)
    
    // Хранит информацию о видимых в данный момент оверлеях
    private val visibleOverlays = HashSet<Int>(MAX_ACTIVE_OVERLAYS)

    // Флаг для отслеживания активности - предотвращает утечки памяти
    @Volatile
    private var isActive = true
    
    // Время последней очистки кэша для автоматической очистки
    private var lastCleanupTime = System.currentTimeMillis()

    init {
        // Запускаем периодическую очистку в фоне
        startMemoryCleanupJob()
    }
    
    /**
     * Запускает задачу периодической очистки памяти
     */
    private fun startMemoryCleanupJob() {
        scope.launch {
            while (isActive) {
                try {
                    delay(CLEANUP_INTERVAL_MS.toLong())
                    cleanupMemory()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при очистке памяти: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Очищает неиспользуемые ресурсы и выполняет оптимизацию памяти
     */
    private fun cleanupMemory() {
        if (!isActive) return
        
        try {
            // Сокращаем списки если они стали слишком большими
            if (allMappings.size() > MAX_ACTIVE_OVERLAYS * 2) {
                val keysToRemove = mutableListOf<Int>()
                
                // Находим ключи, которые не в приоритетном списке
                allMappings.forEach { key, _ ->
                    if (!keysByPriority.contains(key) && !visibleOverlays.contains(key)) {
                        keysToRemove.add(key)
                    }
                }
                
                // Удаляем лишние маппинги
                keysToRemove.forEach { key ->
                    allMappings.remove(key)
                }
                
                Log.d(TAG, "Очищены неиспользуемые маппинги: ${keysToRemove.size}")
            }
            
            // Фиксируем время последней очистки
            lastCleanupTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке памяти: ${e.message}")
        }
    }

    /**
     * Установка состояния активности менеджера
     */
    fun setActive(active: Boolean) {
        isActive = active
        if (!active) {
            clearAll()
        }
    }

    /**
     * Устанавливает все маппинги
     */
    fun setMappings(mappings: Map<Int, Pair<Float, Float>>) {
        if (!isActive) return
        
        scope.launch {
            // Очищаем текущие маппинги
            allMappings.clear()
            keysByPriority.clear()
            
            // Заполняем SparseArray новыми маппингами и сортируем по приоритету
            mappings.forEach { (keyCode, position) ->
                allMappings.put(keyCode, position)
                keysByPriority.add(keyCode)
            }
            
            // Предварительно сортируем по приоритету для быстрого доступа
            keysByPriority.sortWith(compareBy { mappings[it]?.first ?: 0f })

            withContext(Dispatchers.Main) {
                // Сбрасываем видимые оверлеи
                overlayManager.removeAllOverlayButtons()
                visibleOverlays.clear()

                // Показываем только необходимое количество оверлеев
                showPriorityOverlays()
            }
        }
    }

    /**
     * Добавляет маппинг
     */
    fun addMapping(keyCode: Int, x: Float, y: Float) {
        if (!isActive) return
        
        allMappings.put(keyCode, Pair(x, y))
        
        // Добавляем в список приоритетов
        if (!keysByPriority.contains(keyCode)) {
            keysByPriority.add(keyCode)
        }

        // Если оверлеев меньше максимума, показываем новый
        if (visibleOverlays.size < MAX_ACTIVE_OVERLAYS) {
            showOverlay(keyCode)
        }
    }

    /**
     * Удаляет маппинг
     */
    fun removeMapping(keyCode: Int) {
        if (!isActive) return
        
        allMappings.remove(keyCode)
        keysByPriority.remove(keyCode)

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
        if (!isActive) return
        
        allMappings.put(keyCode, Pair(x, y))

        // Если оверлей видимый, обновляем его позицию
        if (visibleOverlays.contains(keyCode)) {
            overlayManager.updateOverlayButtonPosition(keyCode, x, y)
        }
    }

    /**
     * Обработчик нажатия на кнопку оверлея
     */
    private fun onButtonClick(keyCode: Int) {
        if (isActive) {
            val inputHandler = AppModule.getInputEventHandler()
            inputHandler.injectKeyPress(keyCode)
        }
    }

    /**
     * Показывает оверлей для указанного keyCode
     */
    private fun showOverlay(keyCode: Int) {
        if (!isActive) return
        
        try {
            // Проверяем, существует ли маппинг
            val mapping = allMappings.get(keyCode)
            if (mapping == null) {
                Log.w(TAG, "Попытка показать оверлей для несуществующего маппинга с keyCode $keyCode")
                return
            }
            
            val (x, y) = mapping
            // Добавляем кнопку оверлея, если маппинг есть
            overlayManager.addOverlayButton(keyCode, x, y) { code ->
                // Обработка нажатия
                onButtonClick(code)
            }
            // Помечаем оверлей как видимый
            visibleOverlays.add(keyCode)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отображении оверлея с keyCode $keyCode: ${e.message}", e)
            errorHandler?.logError(e, "Ошибка при отображении оверлея")
        }
    }

    /**
     * Показывает оверлеи с наивысшим приоритетом
     */
    private fun showPriorityOverlays() {
        if (!isActive) return
        
        try {
            // Очищаем все видимые оверлеи
            overlayManager.removeAllOverlayButtons()
            visibleOverlays.clear()

            // Показываем только MAX_ACTIVE_OVERLAYS оверлеев по приоритету
            keysByPriority.take(MAX_ACTIVE_OVERLAYS).forEach { keyCode ->
                showOverlay(keyCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отображении приоритетных оверлеев: ${e.message}", e)
            errorHandler?.logError(e, "Ошибка при отображении приоритетных оверлеев")
        }
    }

    /**
     * Показывает следующий приоритетный оверлей
     */
    private fun showNextPriorityOverlay() {
        if (!isActive) return
        
        try {
            // Находим первый keyCode по приоритету, который не отображается
            val nextKeyCode = keysByPriority.firstOrNull {
                !visibleOverlays.contains(it) && allMappings.get(it) != null
            } ?: return

            showOverlay(nextKeyCode)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отображении следующего оверлея: ${e.message}", e)
            errorHandler?.logError(e, "Ошибка при отображении следующего оверлея")
        }
    }

    /**
     * Очищает все оверлеи и маппинги
     */
    fun clearAll() {
        try {
            overlayManager.removeAllOverlayButtons()
            visibleOverlays.clear()
            allMappings.clear()
            keysByPriority.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке оверлеев: ${e.message}", e)
            errorHandler?.logError(e, "Ошибка при очистке оверлеев")
        }
    }

    /**
     * Временно скрывает все оверлеи
     */
    fun hideAllOverlays() {
        if (!isActive) return
        
        try {
            overlayManager.removeAllOverlayButtons()
            visibleOverlays.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при скрытии оверлеев: ${e.message}", e)
            errorHandler?.logError(e, "Ошибка при скрытии оверлеев")
        }
    }

    /**
     * Восстанавливает видимость оверлеев
     */
    fun restoreOverlays() {
        if (!isActive) return
        
        showPriorityOverlays()
    }
    
    /**
     * Освобождает ресурсы при уничтожении
     */
    fun destroy() {
        isActive = false
        clearAll()
        
        try {
            // Отменяем все корутины - удаляем вызов cancelChildren
            scope.cancel()
            
            // Очищаем коллекции для уменьшения утечек памяти
            allMappings.clear()
            keysByPriority.clear()
            visibleOverlays.clear()
            
            // Принудительный запуск сборщика мусора
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при уничтожении менеджера оверлеев: ${e.message}")
        }
    }
}
