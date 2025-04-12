package com.example.gamemapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.helpers.ExternalDeviceDetector
import com.example.gamemapper.helpers.InputEventHandler
import com.example.gamemapper.helpers.MemoryOptimizedOverlayManager
import com.example.gamemapper.helpers.NotificationHelper
import com.example.gamemapper.repository.interfaces.IMappingRepository
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Сервис для обработки событий ввода и отображения оверлеев
 */
class MappingService : AccessibilityService(), InputManager.InputDeviceListener {

    private lateinit var profileRepository: IProfileRepository
    private lateinit var mappingRepository: IMappingRepository
    private lateinit var settingsRepository: ISettingsRepository

    private lateinit var overlayManager: MemoryOptimizedOverlayManager
    private lateinit var inputEventHandler: InputEventHandler
    private lateinit var externalDeviceDetector: ExternalDeviceDetector
    private lateinit var notificationHelper: NotificationHelper

    private var currentProfile: GameProfile? = null
    private var isServiceActive = false

    // Корутины
    val scope = CoroutineScope(Dispatchers.Main + Job())

    // Binder для привязки активности к сервису
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MappingService = this@MappingService
    }

    companion object {
        private const val TAG = "MappingService"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onServiceConnected() {
        try {
            // Инициализация зависимостей
            profileRepository = AppModule.getProfileRepository()
            mappingRepository = AppModule.getMappingRepository()
            settingsRepository = AppModule.getSettingsRepository()

            overlayManager = AppModule.getMemoryOptimizedOverlayManager()
            inputEventHandler = AppModule.getInputEventHandler()
            externalDeviceDetector = AppModule.getExternalDeviceDetector()
            notificationHelper = AppModule.getNotificationHelper()

            // Настройка сервиса доступности
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                notificationTimeout = 100
            }
            serviceInfo = info

            // Регистрируем слушатель подключения/отключения устройств
            externalDeviceDetector.registerInputDeviceListener(this)

            // Создаем канал уведомлений
            notificationHelper.createNotificationChannel()

            // Запускаем сервис в режиме переднего плана
            startForeground(NOTIFICATION_ID, notificationHelper.createServiceNotification())

            // Загружаем последний активный профиль
            loadLastActiveProfile()

            isServiceActive = true
            Log.i(TAG, "Service connected successfully")

            // Уведомляем пользователя
            AppModule.getFeedbackHelper().showToast(getString(R.string.service_started))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect service: ${e.message}", e)
            AppModule.getErrorHandler().handleError(
                e,
                getString(R.string.service_start_error)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
        } else if (intent?.action == "LOAD_PROFILE") {
            val profileId = intent.getStringExtra("profile_id")
            profileId?.let { loadProfile(it) }
        }
        return START_STICKY
    }

    /**
     * Загружает последний активный профиль
     */
    private fun loadLastActiveProfile() {
        val profileId = settingsRepository.getLastActiveProfileId()
        if (profileId != null) {
            loadProfile(profileId)
        }
    }

    /**
     * Загружает профиль по ID
     */
    private fun loadProfile(profileId: String) {
        val profile = profileRepository.getProfileById(profileId)
        if (profile != null) {
            currentProfile = profile

            // Обновляем маппинги из профиля
            overlayManager.setMappings(profile.keyMappings)

            // Уведомляем пользователя
            AppModule.getFeedbackHelper().showToast(
                getString(R.string.profile_loaded, profile.name)
            )
        }
    }

    /**
     * Обновляет маппинги клавиш
     */
    fun updateKeyMappings(mappings: Map<Int, Pair<Float, Float>>) {
        overlayManager.setMappings(mappings)

        // Если есть активный профиль, обновляем его
        currentProfile?.let { profile ->
            profile.keyMappings.clear()
            profile.keyMappings.putAll(mappings)
            profileRepository.saveProfile(profile)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Удаляем все оверлеи
        overlayManager.clearAll()

        // Отменяем регистрацию слушателя устройств
        externalDeviceDetector.unregisterInputDeviceListener(this)

        // Отменяем все корутины
        scope.cancel()

        isServiceActive = false
        Log.i(TAG, "Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Обрабатываем события доступности по необходимости
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Обрабатываем только события от внешних устройств (геймпад, клавиатура)
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            event.source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {

            // Проверяем, что это не виртуальная клавиатура
            val device = event.device
            if (device != null && device.keyboardType == InputDevice.KEYBOARD_TYPE_VIRTUAL) {
                return false
            }

            val keyCode = event.keyCode

            // Если есть маппинг для этой кнопки, обрабатываем его
            if (currentProfile?.keyMappings?.containsKey(keyCode) == true) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    // Инжектируем нажатие клавиши
                    inputEventHandler.injectKeyPress(keyCode)

                    // Вибрация для обратной связи
                    AppModule.getFeedbackHelper().vibrate(50)
                }

                // Возвращаем true, чтобы указать, что событие обработано
                return true
            }
        }

        // Возвращаем false для необработанных событий
        return false
    }

    /**
     * Блокируем события сенсорного экрана, чтобы приложение работало только
     * с внешними устройствами (клавиатура, мышь)
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Определяем источник события
        val source = event.source

        // Пропускаем события от мыши, но блокируем от сенсорного экрана
        if (source and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
            // Блокируем события сенсорного экрана
            return true
        }

        // Для событий от мыши и других устройств возвращаем false,
        // чтобы они обрабатывались стандартным образом
        return false
    }

    /**
     * Обработка подключения нового устройства ввода
     */
    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId)
        Log.d(TAG, "Input device added: ${device.name}, id: $deviceId")

        // Проверяем, что это внешнее устройство
        if (device.sources and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE) != 0) {
            // Уведомляем пользователя
            scope.launch {
                AppModule.getFeedbackHelper().showToast(
                    getString(R.string.device_connected, device.name)
                )
            }
        }
    }

    /**
     * Обработка отключения устройства ввода
     */
    override fun onInputDeviceRemoved(deviceId: Int) {
        Log.d(TAG, "Input device removed: id: $deviceId")

        // Уведомляем пользователя
        scope.launch {
            AppModule.getFeedbackHelper().showToast(
                getString(R.string.device_disconnected)
            )
        }
    }

    /**
     * Обработка изменения устройства ввода
     */
    override fun onInputDeviceChanged(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId)
        Log.d(TAG, "Input device changed: ${device.name}, id: $deviceId")
    }
}
