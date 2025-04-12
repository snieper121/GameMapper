package com.example.gamemapper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository

/**
 * Фабрика для создания ProfileViewModel
 */
class ProfileViewModelFactory(
    private val profileRepository: IProfileRepository,
    private val settingsRepository: ISettingsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(profileRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
