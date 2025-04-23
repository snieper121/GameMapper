package com.example.gamemapper.di

import android.content.Context
import com.example.gamemapper.PersistenceManager
import com.example.gamemapper.helpers.ErrorHandler
import com.example.gamemapper.helpers.ExternalDeviceDetector
import com.example.gamemapper.helpers.FeedbackHelper
import com.example.gamemapper.helpers.InputEventHandler
import com.example.gamemapper.helpers.MemoryOptimizedOverlayManager
import com.example.gamemapper.helpers.NotificationHelper
import com.example.gamemapper.helpers.OverlayManager
import com.example.gamemapper.helpers.PermissionHelper
import com.example.gamemapper.repository.MappingRepository
import com.example.gamemapper.repository.ProfileRepository
import com.example.gamemapper.repository.SettingsRepository
import com.example.gamemapper.repository.interfaces.IMappingRepository
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import com.example.gamemapper.viewmodel.ProfileViewModelFactory

/**
 * Контейнер для зависимостей приложения (простая реализация DI)
 */
class AppContainer(private val applicationContext: Context) {

    // Инициализация PersistenceManager
    init {
        PersistenceManager.init(applicationContext)
    }

    // Репозитории
    val profileRepository: IProfileRepository by lazy {
        ProfileRepository(applicationContext)
    }

    val mappingRepository: IMappingRepository by lazy {
        MappingRepository(applicationContext)
    }

    val settingsRepository: ISettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    // Хелперы
    val errorHandler by lazy { ErrorHandler(applicationContext) }

    val permissionHelper by lazy { PermissionHelper(applicationContext) }

    val feedbackHelper by lazy { FeedbackHelper(applicationContext) }

    val externalDeviceDetector by lazy { ExternalDeviceDetector(applicationContext) }

    val notificationHelper by lazy { NotificationHelper(applicationContext) }

    // Менеджеры
    val overlayManager by lazy { OverlayManager(applicationContext) }

    val memoryOptimizedOverlayManager by lazy {
        MemoryOptimizedOverlayManager(applicationContext, overlayManager)
    }

    val inputEventHandler by lazy {
        InputEventHandler(applicationContext, mappingRepository)
    }

    // ViewModel фабрики
    val profileViewModelFactory by lazy {
        ProfileViewModelFactory(profileRepository, settingsRepository)
    }
}
