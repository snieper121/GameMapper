package com.example.gamemapper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.InputDevice
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.helpers.FeedbackHelper
import com.example.gamemapper.helpers.ExternalDeviceDetector

class GamepadConfigActivity : AppCompatActivity() {

    private lateinit var assignButton: Button
    private lateinit var gamepadListView: RecyclerView
    private lateinit var adapter: GamepadButtonAdapter
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var externalDeviceDetector: ExternalDeviceDetector

    private var awaitingKey = false
    private var currentX = 500f
    private var currentY = 500f

    // Список назначенных кнопок
    private val assignedButtons = mutableMapOf<Int, Pair<Float, Float>>()

    // Для связи с сервисом
    private var mappingService: MappingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MappingService.LocalBinder
            mappingService = binder.getService()
            bound = true

            // Загружаем текущие назначения кнопок из сервиса
            loadCurrentMappings()
            adapter.notifyDataSetChanged()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mappingService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamepad_config)

        // Инициализация зависимостей
        feedbackHelper = AppModule.getFeedbackHelper()
        externalDeviceDetector = AppModule.getExternalDeviceDetector()

        assignButton = findViewById(R.id.assignButton)
        gamepadListView = findViewById(R.id.gamepadListView)

        // Привязка к сервису
        val intent = Intent(this, MappingService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        adapter = GamepadButtonAdapter(assignedButtons) { keyCode ->
            // Удаляем назначение
            assignedButtons.remove(keyCode)
            updateServiceMappings()
            adapter.notifyDataSetChanged()
        }

        gamepadListView.adapter = adapter
        gamepadListView.layoutManager = LinearLayoutManager(this)

        // Добавляем анимацию
        val layoutAnimation = android.view.animation.LayoutAnimationController(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
        )
        layoutAnimation.delay = 0.15f
        layoutAnimation.order = android.view.animation.LayoutAnimationController.ORDER_NORMAL
        gamepadListView.layoutAnimation = layoutAnimation

        assignButton.setOnClickListener {
            // Проверяем, подключен ли геймпад
            if (externalDeviceDetector.isGamepadConnected()) {
                showPositionDialog()
            } else {
                feedbackHelper.showToast(getString(R.string.no_gamepad_connected))
            }
        }
    }

    private fun showPositionDialog() {
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
                    feedbackHelper.showToast(getString(R.string.invalid_position))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        if (!bound || mappingService == null) return

        // Добавляем проверку на null для keyMappings
        mappingService?.keyMappings?.let { mappings ->
            mappings.forEach { (keyCode, position) ->
                // Добавляем только кнопки геймпада
                if (keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE) {
                    assignedButtons[keyCode] = position
                }
            }
        }
    }

    private fun updateServiceMappings() {
        if (!bound || mappingService == null) return

        mappingService?.let {
            // Удаляем старые назначения кнопок геймпада
            val keysToRemove = it.keyMappings.keys.filter { keyCode ->
                keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE
            }
            keysToRemove.forEach { keyCode -> it.keyMappings.remove(keyCode) }

            // Добавляем новые назначения
            it.keyMappings.putAll(assignedButtons)

            // Обновляем оверлеи
            it.updateKeyMappings(it.keyMappings)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    override fun finish() {
        super.finish()
        // Добавляем анимацию при закрытии активности
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

