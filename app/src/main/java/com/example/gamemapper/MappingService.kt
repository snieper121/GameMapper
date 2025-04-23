package com.example.gamemapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.input.InputManager
import android.os.Build
import android.os.SystemClock
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
import kotlinx.coroutines.withContext
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE

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

    // Защита от дребезга кнопок
    private val lastKeyPressTime = mutableMapOf<Int, Long>()
    private val KEY_DEBOUNCE_TIME_MS = 100L // 100 мс для защиты от дребезга
    
    // Публичное свойство для доступа к маппингам
    val keyMappings: MutableMap<Int, Pair<Float, Float>>
        get() = currentProfile?.keyMappings ?: mutableMapOf()

    // Корутины
    val scope = CoroutineScope(Dispatchers.Default + Job())

    companion object {
        private const val TAG = "MappingService"
        private const val NOTIFICATION_ID = 1
    }

    override fun onServiceConnected() {
        try {
            // Инициализация зависимостей
            try {
                profileRepository = AppModule.getProfileRepository()
                mappingRepository = AppModule.getMappingRepository()
                settingsRepository = AppModule.getSettingsRepository()
    
                overlayManager = AppModule.getMemoryOptimizedOverlayManager()
                inputEventHandler = AppModule.getInputEventHandler()
                externalDeviceDetector = AppModule.getExternalDeviceDetector()
                notificationHelper = AppModule.getNotificationHelper()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при инициализации зависимостей: ${e.message}", e)
                throw e
            }
    
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
            val notification = notificationHelper.createServiceNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29)
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                // Более старые версии Android
                startForeground(NOTIFICATION_ID, notification)
            }
    
            // Загружаем маппинги из репозитория
            val initialMappings = mappingRepository.getKeyMappings()
            overlayManager.setMappings(initialMappings)
    
            // Загружаем последний активный профиль
            loadLastActiveProfile()
    
            isServiceActive = true
            Log.i(TAG, getString(R.string.log_service_connected))
    
            // Уведомляем пользователя
            AppModule.getFeedbackHelper().showToast(getString(R.string.service_started))
    
            // Запускаем периодическую проверку изменений маппингов
            startMappingSync()
    
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске сервиса: ${e.message}", e)
            AppModule.getErrorHandler().handleError(
                e,
                getString(R.string.service_start_error)
            )
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP_SERVICE" -> {
                stopSelf()
            }
            "LOAD_PROFILE" -> {
                val profileId = intent.getStringExtra("profile_id")
                profileId?.let { loadProfile(it) }
            }
            "UPDATE_MAPPINGS" -> {
                val mappings = intent.getSerializableExtra("mappings") as? HashMap<Int, Pair<Float, Float>>
                mappings?.let { updateKeyMappings(it) }
            }
        }
        return START_STICKY
    }

    /**
     * Периодически проверяет изменения в маппингах через репозиторий
     */
    private fun startMappingSync() {
        scope.launch {
            while (isServiceActive) {
                val repoMappings = mappingRepository.getKeyMappings()
                val currentMappings = currentProfile?.keyMappings ?: mutableMapOf()
                if (repoMappings != currentMappings) {
                    updateKeyMappings(repoMappings)
                    Log.d(TAG, "Mappings updated from repository")
                }
                delay(1000) // Проверяем каждую секунду
            }
        }
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

            // Сохраняем маппинги в репозиторий
            mappingRepository.updateKeyMappings(profile.keyMappings)

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
            // Сохраняем маппинги в репозиторий
            mappingRepository.updateKeyMappings(mappings)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, getString(R.string.log_service_interrupted))
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Отменяем регистрацию слушателя устройств
            externalDeviceDetector.unregisterInputDeviceListener(this)

            // Деактивируем оверлей менеджер
            overlayManager.setActive(false)
            
            // Удаляем все оверлеи
            overlayManager.clearAll()

            // Отменяем все корутины и освобождаем ресурсы
            scope.cancel()

            isServiceActive = false
            Log.i(TAG, getString(R.string.log_service_destroyed))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при уничтожении сервиса: ${e.message}", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Обрабатываем события доступности по необходимости
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            // Проверка на null и активность сервиса
            if (event == null || !isServiceActive) return false
            
            // Обрабатываем только события от внешних устройств (геймпад, клавиатура)
            if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                event.source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {

                val keyCode = event.keyCode

                // Если есть маппинг для этой кнопки, обрабатываем его
                if (currentProfile?.keyMappings?.containsKey(keyCode) == true) {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        // Проверяем время с последнего нажатия этой кнопки
                        val currentTime = SystemClock.elapsedRealtime()
                        val lastTime = lastKeyPressTime[keyCode] ?: 0L
                        
                        // Если прошло достаточно времени с последнего нажатия
                        if (currentTime - lastTime > KEY_DEBOUNCE_TIME_MS) {
                            try {
                                // Запоминаем время нажатия
                                lastKeyPressTime[keyCode] = currentTime
                                
                                // Используем корутину для операций IO
                                scope.launch(Dispatchers.IO) {
                                    // Инжектируем нажатие клавиши
                                    inputEventHandler.injectKeyPress(keyCode)

                                    // Вибрация для обратной связи
                                    withContext(Dispatchers.Main) {
                                        if (isServiceActive) {
                                            AppModule.getFeedbackHelper().vibrate(50)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, getString(R.string.log_error_key_event, e.message), e)
                            }
                        } else {
                            // Игнорируем слишком частые нажатия (дребезг)
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, getString(R.string.log_ignoring_key, keyCode))
                            }
                        }
                    }
                    
                    // Возвращаем true, чтобы указать, что событие обработано
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.log_error_key_event, e.message), e)
        }

        // Возвращаем false для необработанных событий
        return false
    }

    /**
     * Обрабатывает события касания экрана
     */
    fun handleTouchEvent(event: MotionEvent): Boolean {
        // Получаем источник события
        val source = event.source
        
        // Проверяем настройки: должны ли мы блокировать сенсорный экран
        val shouldBlockTouchscreen = settingsRepository.isBlockTouchEventsEnabled()
        
        // Если настройка включена и событие от сенсорного экрана - блокируем
        if (shouldBlockTouchscreen && 
            source and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
            return true // Блокируем событие
        }
        
        // В остальных случаях пропускаем событие
        return false
    }

    /**
     * Обработка подключения нового устройства ввода
     */
    override fun onInputDeviceAdded(deviceId: Int) {
        try {
            val device = InputDevice.getDevice(deviceId) ?: return
            Log.d(TAG, getString(R.string.log_device_added, device.name, deviceId))

            // Проверяем, что это внешнее устройство
            if (device.sources and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE) != 0) {
                // Уведомляем пользователя
                scope.launch(kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                    Log.e(TAG, getString(R.string.log_error_notification, e.message), e)
                }) {
                    AppModule.getFeedbackHelper().showToast(
                        getString(R.string.device_connected, device.name)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.log_error_processing_device, e.message), e)
        }
    }

    /**
     * Обработка отключения устройства ввода
     */
    override fun onInputDeviceRemoved(deviceId: Int) {
        try {
            Log.d(TAG, getString(R.string.log_device_removed, deviceId))

            // Уведомляем пользователя
            scope.launch(kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                Log.e(TAG, getString(R.string.log_error_notification, e.message), e)
            }) {
                AppModule.getFeedbackHelper().showToast(
                    getString(R.string.device_disconnected)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.log_error_processing_device, e.message), e)
        }
    }

    /**
     * Обработка изменения устройства ввода
     */
    override fun onInputDeviceChanged(deviceId: Int) {
        try {
            val device = InputDevice.getDevice(deviceId) ?: return
            Log.d(TAG, getString(R.string.log_device_changed, device.name, deviceId))
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.log_error_processing_device, e.message), e)
        }
    }

    // Добавляем вспомогательный метод для проверки виртуальной клавиатуры
    private fun InputDevice.isVirtual(): Boolean {
        return this.supportsSource(InputDevice.SOURCE_KEYBOARD) && 
               !this.isExternal
    }
}