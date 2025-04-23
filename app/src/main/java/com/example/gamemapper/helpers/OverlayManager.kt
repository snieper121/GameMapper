package com.example.gamemapper.helpers

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.gamemapper.ButtonSettingsDialog
import com.example.gamemapper.CustomOverlayButton
import com.example.gamemapper.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Менеджер оверлеев для отображения кнопок поверх других приложений
 */
class OverlayManager(private val context: Context) {

    // Флаг активности для управления жизненным циклом
    @Volatile
    private var isActive = true

    private val windowManager: WindowManager by lazy {
        try {
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: throw IllegalStateException("Window service not available")
        } catch (e: Exception) {
            Log.e("OverlayManager", "Ошибка при получении WindowManager", e)
            throw RuntimeException("Не удалось получить WindowManager", e)
        }
    }

    // Инициализируем errorHandler после того, как будет инициализирован AppModule
    private val errorHandler by lazy { 
        try {
            AppModule.getErrorHandler()
        } catch (e: Exception) {
            Log.e("OverlayManager", "Ошибка при получении ErrorHandler", e)
            null
        }
    }
    
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
        if (!isActive) return false
        
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
            errorHandler?.handleError(
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
        if (!isActive) return false
        
        return try {
            val view = overlayViews[keyCode] ?: return false

            // Проверяем, что view действительно добавлена в WindowManager
            try {
                windowManager.removeView(view)
            } catch (e: IllegalArgumentException) {
                // View может быть уже удалена или не добавлена
                Log.w("OverlayManager", "View для keyCode $keyCode не была найдена в WindowManager: ${e.message}")
            }
            
            overlayViews.remove(keyCode)
            true
        } catch (e: Exception) {
            errorHandler?.logError(e, "Error removing overlay button for keyCode $keyCode: ${e.message}")
            false
        }
    }

    /**
     * Удаляет все кнопки-оверлеи
     */
    fun removeAllOverlayButtons() {
        if (!isActive) return
        
        try {
            val keys = overlayViews.keys.toList()
            for (keyCode in keys) {
                try {
                    removeOverlayButton(keyCode)
                } catch (e: Exception) {
                    // Логируем ошибку, но продолжаем удалять остальные кнопки
                    errorHandler?.logError(e, "Ошибка при удалении кнопки с keyCode $keyCode")
                }
            }
        } catch (e: Exception) {
            errorHandler?.handleError(
                e,
                "Не удалось удалить все кнопки",
                "Error removing all overlay buttons: ${e.message}"
            )
        }
    }

    /**
     * Обновляет позицию кнопки-оверлея
     */
    fun updateOverlayButtonPosition(keyCode: Int, x: Float, y: Float): Boolean {
        if (!isActive) return false
        
        return try {
            val view = overlayViews[keyCode] ?: return false

            val params = view.layoutParams
            if (params !is WindowManager.LayoutParams) {
                Log.e("OverlayManager", "Неверный тип параметров для keyCode $keyCode")
                return false
            }
            
            params.x = x.toInt()
            params.y = y.toInt()

            windowManager.updateViewLayout(view, params)

            true
        } catch (e: Exception) {
            errorHandler?.logError(e, "Error updating overlay button position for keyCode $keyCode: ${e.message}")
            false
        }
    }

    /**
     * Создает кнопку-оверлей
     */
    private fun createOverlayButton(keyCode: Int, onClickListener: (Int) -> Unit): CustomOverlayButton {
        // Получаем настройки для конкретного keyCode из настроек
        val settingsRepository = AppModule.getSettingsRepository()
        val buttonSettings = settingsRepository.getButtonSettings(keyCode)
        
        try {
            // Создаем кнопку с настройками
            val button = CustomOverlayButton(context).apply {
                // Применяем настройки из репозитория, если они есть
                buttonSize = buttonSettings.buttonSize
                buttonText = buttonSettings.buttonText.ifEmpty { android.view.KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "") }
                buttonShape = buttonSettings.buttonShape
                
                // Цвета и эффекты
                buttonColor = buttonSettings.buttonColor
                buttonTextColor = buttonSettings.buttonTextColor
                buttonAlpha = buttonSettings.buttonAlpha
                
                // Граница
                buttonBorderColor = buttonSettings.buttonBorderColor
                buttonBorderWidth = buttonSettings.buttonBorderWidth
                
                // Эффекты
                buttonGradient = buttonSettings.buttonGradient
                buttonShadowRadius = buttonSettings.buttonShadowRadius
                
                // Активируем нажатие
                setOnClickListener {
                    onClickListener(keyCode)
                }
                
                // Добавляем обработчик долгого нажатия для открытия настроек
                setOnLongClickListener {
                    // Открываем диалог настроек кнопки
                    showButtonSettingsDialog(keyCode, this)
                    true
                }
            }
            
            Log.d("OverlayManager", "Кнопка с keyCode $keyCode успешно создана")
            return button
        } catch (e: Exception) {
            Log.e("OverlayManager", "Ошибка при создании кнопки с keyCode $keyCode: ${e.message}")
            // Создаем кнопку с базовыми настройками в случае ошибки
            return CustomOverlayButton(context).apply {
                buttonText = "Err"
                setOnClickListener { onClickListener(keyCode) }
            }
        }
    }
    
    /**
     * Показывает диалог настроек кнопки
     */
    private fun showButtonSettingsDialog(keyCode: Int, button: CustomOverlayButton) {
        try {
            val settingsRepository = AppModule.getSettingsRepository()
            
            // Получаем текущие настройки
            val currentSettings = settingsRepository.getButtonSettings(keyCode)
            
            // Дополняем поле для текста, если оно не было установлено
            if (currentSettings.buttonText.isEmpty()) {
                currentSettings.buttonText = button.buttonText
            }
            
            // Создаем и показываем диалог настроек
            val dialog = ButtonSettingsDialog(
                context,
                currentSettings
            ) { newSettings ->
                try {
                    // Проверяем, что newSettings не null
                    if (newSettings == null) {
                        Log.e("OverlayManager", "Получены null настройки для кнопки с keyCode $keyCode")
                        return@ButtonSettingsDialog
                    }
                    
                    // Сохраняем новые настройки
                    settingsRepository.saveButtonSettings(keyCode, newSettings)
                    
                    // Применяем настройки к кнопке
                    button.buttonSize = newSettings.buttonSize
                    button.buttonColor = newSettings.buttonColor
                    button.buttonShape = newSettings.buttonShape
                    button.buttonTextColor = newSettings.buttonTextColor
                    button.buttonBorderColor = newSettings.buttonBorderColor
                    button.buttonBorderWidth = newSettings.buttonBorderWidth
                    button.buttonAlpha = newSettings.buttonAlpha
                    button.buttonGradient = newSettings.buttonGradient
                    button.buttonShadowRadius = newSettings.buttonShadowRadius
                    
                    // Применяем текст кнопки, если он был задан
                    if (newSettings.buttonText.isNotEmpty()) {
                        button.buttonText = newSettings.buttonText
                    }
                    
                    // Обновляем позицию после изменения размера
                    val overlay = overlayViews[keyCode]
                    if (overlay != null) {
                        val params = overlay.layoutParams
                        if (params is WindowManager.LayoutParams) {
                            windowManager.updateViewLayout(overlay, params)
                        } else {
                            Log.e("OverlayManager", "Неверный тип параметров при обновлении размера для keyCode $keyCode")
                        }
                    }
                } catch (e: Exception) {
                    errorHandler?.handleError(
                        e,
                        "Не удалось применить настройки кнопки",
                        "Error applying button settings for keyCode $keyCode: ${e.message}"
                    )
                }
            }
            
            dialog.show()
        } catch (e: Exception) {
            errorHandler?.handleError(
                e,
                "Не удалось открыть диалог настроек кнопки",
                "Error showing button settings dialog for keyCode $keyCode: ${e.message}"
            )
        }
    }

    /**
     * Создает параметры для размещения кнопки на экране
     */
    private fun createButtonLayoutParams(x: Float, y: Float): WindowManager.LayoutParams {
        // Выбираем тип оверлея в зависимости от версии Android
        val overlayType = when {
            Build.VERSION.SDK_INT >= 26 /* Build.VERSION_CODES.O */ -> 
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            Build.VERSION.SDK_INT >= 24 /* Build.VERSION_CODES.N */ -> 
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            else -> 
                WindowManager.LayoutParams.TYPE_PHONE
        }
        
        // Устанавливаем флаги
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x.toInt()
            this.y = y.toInt()
            // Добавляем дополнительные свойства для лучшего отображения
            alpha = 0.95f
            windowAnimations = android.R.style.Animation_Toast
        }
    }

    /**
     * Асинхронно обновляет все оверлеи
     */
    fun updateAllOverlaysAsync(mappings: Map<Int, Pair<Float, Float>>, onClickListener: (Int) -> Unit) {
        if (!isActive) return
        
        // Пропускаем выполнение, если маппингов нет
        if (mappings.isEmpty()) {
            scope.launch {
                withContext(Dispatchers.Main) {
                    removeAllOverlayButtons()
                }
            }
            return
        }
        
        scope.launch {
            // Удаляем все существующие оверлеи в UI потоке
            withContext(Dispatchers.Main) {
                removeAllOverlayButtons()
            }

            // Добавляем новые оверлеи в UI потоке
            for ((keyCode, position) in mappings) {
                withContext(Dispatchers.Main) {
                    addOverlayButton(keyCode, position.first, position.second, onClickListener)
                }
            }
        }
    }

    /**
     * Освобождает ресурсы и отменяет все операции
     */
    fun destroy() {
        if (!isActive) return
        
        isActive = false
        
        try {
            // Удаляем все оверлеи
            removeAllOverlayButtons()
            
            // Отменяем все корутины
            scope.coroutineContext.cancelChildren()
        } catch (e: Exception) {
            Log.e("OverlayManager", "Ошибка при уничтожении менеджера оверлеев", e)
        }
    }

    /**
     * Проверяет наличие необходимых разрешений для отображения оверлея
     */
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) { // Android M и выше
            try {
                Settings.canDrawOverlays(context)
            } catch (e: Exception) {
                Log.e("OverlayManager", "Ошибка при проверке разрешений оверлея: ${e.message}")
                false
            }
        } else {
            true // Для более ранних версий Android разрешение не требуется
        }
    }

    /**
     * Открывает настройки разрешений оверлея
     */
    fun openOverlayPermissionSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 23) { // Android M и выше
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("OverlayManager", "Не удалось открыть настройки разрешений: ${e.message}")
        }
    }
}
