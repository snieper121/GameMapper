package com.example.gamemapper.helpers

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.gamemapper.CustomOverlayButton
import com.example.gamemapper.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Менеджер оверлеев для отображения кнопок поверх других приложений
 */
class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val errorHandler = AppModule.getErrorHandler()
    private val scope = CoroutineScope(Dispatchers.Main)

    // Хранение всех добавленных оверлеев
    private val overlayViews = mutableMapOf<Int, View>()

    /**
     * Добавляет кнопку-оверлей
     */
    fun addOverlayButton(
        keyCode: Int,
        x: Float,
        y: Float,
        onClickListener: (Int) -> Unit
    ): Boolean {
        return try {
            // Удаляем существующую кнопку с таким же keyCode, если она есть
            removeOverlayButton(keyCode)

            // Создаем новую кнопку
            val button = createOverlayButton(keyCode, onClickListener)

            // Добавляем кнопку на экран
            windowManager.addView(button, createButtonLayoutParams(x, y))

            // Сохраняем ссылку на кнопку
            overlayViews[keyCode] = button

            true
        } catch (e: Exception) {
            errorHandler.handleError(
                e,
                "Не удалось добавить кнопку оверлея",
                "Error adding overlay button for keyCode $keyCode: ${e.message}"
            )
            false
        }
    }

    /**
     * Удаляет кнопку-оверлей
     */
    fun removeOverlayButton(keyCode: Int): Boolean {
        return try {
            val view = overlayViews[keyCode] ?: return false

            windowManager.removeView(view)
            overlayViews.remove(keyCode)

            true
        } catch (e: Exception) {
            errorHandler.logError(e, "Error removing overlay button for keyCode $keyCode: ${e.message}")
            false
        }
    }

    /**
     * Удаляет все кнопки-оверлеи
     */
    fun removeAllOverlayButtons() {
        val keys = overlayViews.keys.toList()
        for (keyCode in keys) {
            removeOverlayButton(keyCode)
        }
    }

    /**
     * Обновляет позицию кнопки-оверлея
     */
    fun updateOverlayButtonPosition(keyCode: Int, x: Float, y: Float): Boolean {
        return try {
            val view = overlayViews[keyCode] ?: return false

            val params = view.layoutParams as WindowManager.LayoutParams
            params.x = x.toInt()
            params.y = y.toInt()

            windowManager.updateViewLayout(view, params)

            true
        } catch (e: Exception) {
            errorHandler.logError(e, "Error updating overlay button position for keyCode $keyCode: ${e.message}")
            false
        }
    }

    /**
     * Создает кнопку-оверлей
     */
    private fun createOverlayButton(keyCode: Int, onClickListener: (Int) -> Unit): CustomOverlayButton {
        return CustomOverlayButton(context).apply {
            buttonSize = 100f
            buttonText = android.view.KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
            buttonShape = CustomOverlayButton.SHAPE_CIRCLE

            setOnClickListener {
                onClickListener(keyCode)
            }
        }
    }

    /**
     * Создает параметры для размещения кнопки на экране
     */
    private fun createButtonLayoutParams(x: Float, y: Float): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x.toInt()
            this.y = y.toInt()
        }
    }

    /**
     * Асинхронно обновляет все оверлеи
     */
    fun updateAllOverlaysAsync(mappings: Map<Int, Pair<Float, Float>>, onClickListener: (Int) -> Unit) {
        scope.launch {
            // Удаляем все существующие оверлеи
            removeAllOverlayButtons()

            // Добавляем новые оверлеи
            for ((keyCode, position) in mappings) {
                addOverlayButton(keyCode, position.first, position.second, onClickListener)
            }
        }
    }
}
