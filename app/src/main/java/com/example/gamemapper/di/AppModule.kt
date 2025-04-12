package com.example.gamemapper.di

import android.content.Context
import com.example.gamemapper.repository.interfaces.IMappingRepository
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository

/**
 * Модуль для предоставления зависимостей
 */
object AppModule {
    private lateinit var appContainer: AppContainer

    fun initialize(context: Context) {
        appContainer = AppContainer(context.applicationContext)
    }

    fun getProfileRepository(): IProfileRepository {
        return appContainer.profileRepository
    }

    fun getMappingRepository(): IMappingRepository {
        return appContainer.mappingRepository
    }

    fun getSettingsRepository(): ISettingsRepository {
        return appContainer.settingsRepository
    }

    fun getErrorHandler() = appContainer.errorHandler

    fun getPermissionHelper() = appContainer.permissionHelper

    fun getFeedbackHelper() = appContainer.feedbackHelper

    fun getExternalDeviceDetector() = appContainer.externalDeviceDetector

    fun getNotificationHelper() = appContainer.notificationHelper

    fun getOverlayManager() = appContainer.overlayManager

    fun getMemoryOptimizedOverlayManager() = appContainer.memoryOptimizedOverlayManager

    fun getInputEventHandler() = appContainer.inputEventHandler

    fun getProfileViewModelFactory() = appContainer.profileViewModelFactory
}
