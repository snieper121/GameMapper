package com.example.gamemapper.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamemapper.GameProfile
import com.example.gamemapper.repository.interfaces.IProfileRepository
import com.example.gamemapper.repository.interfaces.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel для работы с профилями
 */
class ProfileViewModel(
    private val profileRepository: IProfileRepository,
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _profiles = MutableLiveData<List<GameProfile>>()
    val profiles: LiveData<List<GameProfile>> = _profiles

    private val _activeProfile = MutableLiveData<GameProfile?>()
    val activeProfile: LiveData<GameProfile?> = _activeProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadProfiles()
    }

    /**
     * Загружает все профили
     */
    fun loadProfiles() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    val allProfiles = profileRepository.getAllProfiles()

                    // Получаем ID активного профиля
                    val activeProfileId = settingsRepository.getLastActiveProfileId()

                    // Находим активный профиль
                    val active = if (activeProfileId != null) {
                        profileRepository.getProfileById(activeProfileId)
                    } else {
                        null
                    }

                    withContext(Dispatchers.Main) {
                        _profiles.value = allProfiles
                        _activeProfile.value = active
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Создает новый профиль
     */
    fun createProfile(name: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    val newProfile = profileRepository.createProfile(name)

                    // Обновляем список профилей
                    val allProfiles = profileRepository.getAllProfiles()

                    withContext(Dispatchers.Main) {
                        _profiles.value = allProfiles
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Активирует профиль
     */
    fun activateProfile(profileId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Сохраняем ID активного профиля
                    settingsRepository.saveLastActiveProfileId(profileId)

                    // Получаем профиль
                    val profile = profileRepository.getProfileById(profileId)

                    withContext(Dispatchers.Main) {
                        _activeProfile.value = profile
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Обновляет профиль
     */
    fun updateProfile(profile: GameProfile) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    profileRepository.saveProfile(profile)

                    // Обновляем список профилей
                    val allProfiles = profileRepository.getAllProfiles()

                    withContext(Dispatchers.Main) {
                        _profiles.value = allProfiles

                        // Если обновили активный профиль, обновляем и его
                        if (_activeProfile.value?.id == profile.id) {
                            _activeProfile.value = profile
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Удаляет профиль
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    profileRepository.deleteProfile(profileId)

                    // Если удалили активный профиль, сбрасываем его
                    if (_activeProfile.value?.id == profileId) {
                        settingsRepository.saveLastActiveProfileId("")
                    }

                    // Обновляем список профилей
                    val allProfiles = profileRepository.getAllProfiles()

                    withContext(Dispatchers.Main) {
                        _profiles.value = allProfiles

                        // Если удалили активный профиль, сбрасываем его
                        if (_activeProfile.value?.id == profileId) {
                            _activeProfile.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Дублирует профиль
     */
    fun duplicateProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                withContext(Dispatchers.IO) {
                    profileRepository.duplicateProfile(profileId, newName)

                    // Обновляем список профилей
                    val allProfiles = profileRepository.getAllProfiles()

                    withContext(Dispatchers.Main) {
                        _profiles.value = allProfiles
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Сбрасывает ошибку
     */
    fun resetError() {
        _error.value = null
    }
}
