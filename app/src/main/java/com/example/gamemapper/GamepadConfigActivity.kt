package com.example.gamemapper

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.helpers.FeedbackHelper
import com.example.gamemapper.helpers.ExternalDeviceDetector
import com.example.gamemapper.repository.interfaces.IMappingRepository

class GamepadConfigActivity : AppCompatActivity() {

    private lateinit var assignButton: Button
    private lateinit var gamepadListView: RecyclerView
    private lateinit var adapter: GamepadButtonAdapter
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var externalDeviceDetector: ExternalDeviceDetector
    private lateinit var mappingRepository: IMappingRepository

    private var awaitingKey = false
    private var currentX = 500f
    private var currentY = 500f

    // Список назначенных кнопок
    private val assignedButtons = mutableMapOf<Int, Pair<Float, Float>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamepad_config)

        // Восстанавливаем сохраненное состояние, если есть
        if (savedInstanceState != null) {
            currentX = savedInstanceState.getFloat(KEY_CURRENT_X, 500f)
            currentY = savedInstanceState.getFloat(KEY_CURRENT_Y, 500f)
            awaitingKey = savedInstanceState.getBoolean(KEY_AWAITING_KEY, false)
        }

        // Инициализация зависимостей
        feedbackHelper = AppModule.getFeedbackHelper()
        externalDeviceDetector = AppModule.getExternalDeviceDetector()
        mappingRepository = AppModule.getMappingRepository()

        assignButton = findViewById(R.id.assignButton)
        gamepadListView = findViewById(R.id.gamepadListView)

        // Создаем адаптер с оптимизированным кэшированием видов
        adapter = GamepadButtonAdapter(assignedButtons) { keyCode ->
            // Удаляем назначение
            assignedButtons.remove(keyCode)
            updateServiceMappings()
            adapter.notifyDataSetChanged()
        }

        gamepadListView.adapter = adapter
        gamepadListView.layoutManager = LinearLayoutManager(this)
        
        // Включаем кэширование для улучшения производительности
        gamepadListView.setHasFixedSize(true)
        gamepadListView.setItemViewCacheSize(10)

        // Добавляем анимацию с меньшей задержкой для более быстрого отображения
        val layoutAnimation = android.view.animation.LayoutAnimationController(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
        )
        layoutAnimation.delay = 0.1f
        layoutAnimation.order = android.view.animation.LayoutAnimationController.ORDER_NORMAL
        gamepadListView.layoutAnimation = layoutAnimation

        // Загружаем текущие маппинги из репозитория
        loadCurrentMappings()
        adapter.notifyDataSetChanged()

        assignButton.setOnClickListener {
            // Проверяем, подключен ли геймпад
            if (externalDeviceDetector.isGamepadConnected()) {
                showPositionDialog()
            } else {
                feedbackHelper.showToast(getString(R.string.no_gamepad_connected))
            }
        }
    }

    /**
     * Сохраняем состояние активности
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(KEY_CURRENT_X, currentX)
        outState.putFloat(KEY_CURRENT_Y, currentY)
        outState.putBoolean(KEY_AWAITING_KEY, awaitingKey)
    }

    /**
     * Метод для безопасного отображения диалога с проверками на состояние активности
     */
    private fun showPositionDialog() {
        if (isFinishing || isDestroyed) return
        
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_position_input, null)
            val xInput = dialogView.findViewById<android.widget.TextView>(R.id.xPositionInput)
            val yInput = dialogView.findViewById<android.widget.TextView>(R.id.yPositionInput)

            xInput.text = currentX.toString()
            yInput.text = currentY.toString()

            android.app.AlertDialog.Builder(this)
                .setTitle(R.string.set_position)
                .setView(dialogView)
                .setPositiveButton(R.string.ok) { _, _ ->
                    try {
                        currentX = xInput.text.toString().toFloatOrNull() ?: 500f
                        currentY = yInput.text.toString().toFloatOrNull() ?: 500f

                        awaitingKey = true
                        feedbackHelper.showToast(getString(R.string.press_gamepad_button))
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при обработке ввода: ${e.message}", e)
                        feedbackHelper.showToast(getString(R.string.invalid_position))
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отображении диалога: ${e.message}", e)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (awaitingKey && event?.source?.and(InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            assignedButtons[keyCode] = Pair(currentX, currentY)
            updateServiceMappings()
            adapter.notifyDataSetChanged()

            feedbackHelper.showToast(
                getString(R.string.button_assigned, KeyEvent.keyCodeToString(keyCode))
            )

            awaitingKey = false
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadCurrentMappings() {
        // Получаем маппинги из репозитория
        val currentMappings = mappingRepository.getKeyMappings()
        // Фильтруем только кнопки геймпада
        currentMappings.forEach { (keyCode, position) ->
            if (keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE) {
                assignedButtons[keyCode] = position
            }
        }
    }

    private fun updateServiceMappings() {
        // Получаем текущие маппинги из репозитория
        val currentMappings = mappingRepository.getKeyMappings().toMutableMap()
        
        // Удаляем только маппинги, связанные с геймпадом
        currentMappings.keys
            .filter { keyCode ->
                keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE
            }
            .forEach { keyCode ->
                currentMappings.remove(keyCode)
            }

        // Добавляем новые маппинги геймпада
        currentMappings.putAll(assignedButtons)

        // Сохраняем обновленные маппинги в репозиторий
        mappingRepository.updateKeyMappings(currentMappings)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        // Добавляем анимацию при закрытии активности
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        private const val TAG = "GamepadConfigActivity"
        private const val KEY_CURRENT_X = "current_x"
        private const val KEY_CURRENT_Y = "current_y"
        private const val KEY_AWAITING_KEY = "awaiting_key"
    }
}