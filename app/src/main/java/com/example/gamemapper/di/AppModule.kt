package com.example.gamemapper.di

import android.content.Context
import com.example.gamemapper.repository.interfaces.IMappingRepository
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Модуль для предоставления зависимостей
 */
object AppModule {
    private lateinit var appContainer: AppContainer
    private val initialized = AtomicBoolean(false)
    
    @Synchronized
    fun initialize(context: Context) {
        if (initialized.get()) return
        appContainer = AppContainer(context.applicationContext)
        initialized.set(true)
    }
    
    private fun checkInitialization() {
        check(initialized.get()) { "AppModule должен быть инициализирован перед использованием" }
    }

    fun getProfileRepository(): IProfileRepository {
        checkInitialization()
        return appContainer.profileRepository
    }

    fun getMappingRepository(): IMappingRepository {
        checkInitialization()
        return appContainer.mappingRepository
    }

    fun getSettingsRepository(): ISettingsRepository {
        checkInitialization()
        return appContainer.settingsRepository
    }

    fun getErrorHandler() = appContainer.errorHandler.also { checkInitialization() }

    fun getPermissionHelper() = appContainer.permissionHelper.also { checkInitialization() }

    fun getFeedbackHelper() = appContainer.feedbackHelper.also { checkInitialization() }

    fun getExternalDeviceDetector() = appContainer.externalDeviceDetector.also { checkInitialization() }

    fun getNotificationHelper() = appContainer.notificationHelper.also { checkInitialization() }

    fun getOverlayManager() = appContainer.overlayManager.also { checkInitialization() }

    fun getMemoryOptimizedOverlayManager() = appContainer.memoryOptimizedOverlayManager.also { checkInitialization() }

    fun getInputEventHandler() = appContainer.inputEventHandler.also { checkInitialization() }

    fun getProfileViewModelFactory() = appContainer.profileViewModelFactory.also { checkInitialization() }
}
