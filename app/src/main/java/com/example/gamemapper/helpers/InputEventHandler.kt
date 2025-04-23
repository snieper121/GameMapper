package com.example.gamemapper.helpers

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.gamemapper.repository.interfaces.IMappingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Обработчик событий ввода от внешних устройств
 */
class InputEventHandler(
    context: Context,
    private val mappingRepository: IMappingRepository
) {

    companion object {
        private const val TAG = "InputEventHandler"
        private const val KEY_PRESS_CACHE_SIZE = 50
    }

    // Используем WeakReference для избежания утечек памяти
    private val contextRef = WeakReference(context)
    
    // Флаг активности
    private val isActive = AtomicBoolean(true)
    
    // Кэширование для ускорения частых запросов
    private val keyMappingsCache = LruCache<Int, Pair<Float, Float>>(KEY_PRESS_CACHE_SIZE)

    private val inputManager: InputManager by lazy {
        contextRef.get()?.getSystemService(Context.INPUT_SERVICE) as? InputManager
            ?: throw IllegalStateException("Не удалось получить InputManager")
    }

    private var injectInputEventMethod: Method? = null

    init {
        try {
            injectInputEventMethod = InputManager::class.java.getDeclaredMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.java
            ).apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get injectInputEvent method: ${e.message}", e)
        }
    }
    
    /**
     * Устанавливает состояние активности
     */
    fun setActive(active: Boolean) {
        isActive.set(active)
    }

    /**
     * Инжектирует событие нажатия клавиши
     */
    fun injectKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        if (!isActive.get() || injectInputEventMethod == null) return false
        
        try {
            val now = SystemClock.uptimeMillis()
            val event = KeyEvent(
                now, now,
                if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                keyCode, 0, 0, 
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    KeyCharacterMap.VIRTUAL_KEYBOARD
                } else {
                    0
                }), 
                0,
                KeyEvent.FLAG_FROM_SYSTEM or KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD
            )

            injectInputEventMethod?.invoke(inputManager, event, 0)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting key event: ${e.message}", e)
            return false
        }
    }

    /**
     * Инжектирует событие нажатия и отпускания клавиши
     */
    suspend fun injectKeyPress(keyCode: Int, delay: Long = 50): Boolean {
        if (!isActive.get()) return false
        
        // Переключаемся на IO поток для сетевых/IO операций
        return withContext(Dispatchers.IO) {
            val downSuccess = injectKeyEvent(keyCode, true)

            // Небольшая задержка между нажатием и отпусканием
            SystemClock.sleep(delay)

            val upSuccess = injectKeyEvent(keyCode, false)

            downSuccess && upSuccess
        }
    }
    
    /**
     * Синхронная версия для обратной совместимости
     */
    fun injectKeyPress(keyCode: Int): Boolean {
        if (!isActive.get()) return false
        
        val downSuccess = injectKeyEvent(keyCode, true)

        // Небольшая задержка между нажатием и отпусканием
        SystemClock.sleep(50)

        val upSuccess = injectKeyEvent(keyCode, false)

        return downSuccess && upSuccess
    }

    /**
     * Инжектирует событие касания экрана
     */
    fun injectTouchEvent(
        action: Int,
        x: Float,
        y: Float,
        metaState: Int = 0
    ): Boolean {
        if (!isActive.get() || injectInputEventMethod == null) return false
        
        try {
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(
                now, now,
                action, x, y, 
                0.0f, 0.0f, // pressure и size как float
                0, // metaState как int
                0.0f, 0.0f, // xPrecision и yPrecision как float
                0, 0
            )

            injectInputEventMethod?.invoke(inputManager, event, 0)
            event.recycle()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting touch event: ${e.message}", e)
            return false
        }
    }

    /**
     * Обрабатывает событие клавиши от геймпада
     */
    fun handleGamepadKeyEvent(event: KeyEvent): Boolean {
        if (!isActive.get()) return false
        
        // Проверяем, что событие от геймпада
        if (event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) {
            return false
        }

        val keyCode = event.keyCode
        
        // Проверяем кэш
        var mapping = keyMappingsCache.get(keyCode)
        
        // Если нет в кэше, получаем из репозитория
        if (mapping == null) {
            val mappings = mappingRepository.getKeyMappings()
            mapping = mappings[keyCode]
            
            // Если нашли, добавляем в кэш
            if (mapping != null) {
                keyMappingsCache.put(keyCode, mapping)
            }
        }

        // Если есть маппинг для этой кнопки, обрабатываем его
        if (mapping != null && event.action == KeyEvent.ACTION_DOWN) {
            return injectKeyPress(keyCode)
        }

        return false
    }

    /**
     * Блокирует события касания экрана для работы только с внешними устройствами
     */
    fun blockTouchEvents(event: MotionEvent): Boolean {
        // Блокируем события от сенсорного экрана, но не от мыши
        return event.source and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN
    }
    
    /**
     * Очищает ресурсы при уничтожении
     */
    fun destroy() {
        setActive(false)
        keyMappingsCache.evictAll()
        injectInputEventMethod = null
    }
}
