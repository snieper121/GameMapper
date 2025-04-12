package com.example.gamemapper.helpers

import android.content.Context
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.gamemapper.repository.interfaces.IMappingRepository
import java.lang.reflect.Method

/**
 * Обработчик событий ввода от внешних устройств
 */
class InputEventHandler(
    private val context: Context,
    private val mappingRepository: IMappingRepository
) {

    companion object {
        private const val TAG = "InputEventHandler"
    }

    private val inputManager: InputManager by lazy {
        context.getSystemService(Context.INPUT_SERVICE) as InputManager
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
     * Инжектирует событие нажатия клавиши
     */
    fun injectKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        try {
            val now = SystemClock.uptimeMillis()
            val event = KeyEvent(
                now, now,
                if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
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
    fun injectKeyPress(keyCode: Int, delay: Long = 50): Boolean {
        val downSuccess = injectKeyEvent(keyCode, true)

        // Небольшая задержка между нажатием и отпусканием
        SystemClock.sleep(delay)

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
        try {
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(
                now, now,
                action, x, y, metaState, 1.0f, 0.0f, 0f, 0f,
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
        // Проверяем, что событие от геймпада
        if (event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) {
            return false
        }

        val keyCode = event.keyCode
        val mappings = mappingRepository.getKeyMappings()

        // Если есть маппинг для этой кнопки, обрабатываем его
        if (mappings.containsKey(keyCode) && event.action == KeyEvent.ACTION_DOWN) {
            injectKeyPress(keyCode)
            return true
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
}
