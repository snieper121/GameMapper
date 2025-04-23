package com.example.gamemapper

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemapper.adapters.ProfileDiffCallback
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.helpers.ExternalDeviceDetector
import com.example.gamemapper.helpers.FeedbackHelper
import com.example.gamemapper.helpers.PermissionHelper
import com.example.gamemapper.viewmodel.ProfileViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.annotation.RequiresApi
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var profileList: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addProfileButton: FloatingActionButton
    private lateinit var toggleServiceButton: Button

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var externalDeviceDetector: ExternalDeviceDetector
    
    // Для сохранения состояния при повороте экрана
    private var lastSelectedProfileId: String? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_SELECTED_PROFILE = "selected_profile"
        private const val REQUEST_FOREGROUND_SERVICE_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Восстанавливаем сохраненное состояние, если есть
        if (savedInstanceState != null) {
            lastSelectedProfileId = savedInstanceState.getString(KEY_SELECTED_PROFILE)
            Log.d(TAG, getString(R.string.log_restored_state, lastSelectedProfileId ?: "null"))
        }

        try {
            // Инициализация зависимостей
            permissionHelper = AppModule.getPermissionHelper()
            feedbackHelper = AppModule.getFeedbackHelper()
            externalDeviceDetector = AppModule.getExternalDeviceDetector()

            // Инициализация ViewModel с использованием by lazy для отложенной инициализации
            profileViewModel = ViewModelProvider(
                this,
                AppModule.getProfileViewModelFactory()
            )[ProfileViewModel::class.java]

            // Настройка UI
            setupUI()

            // Настройка наблюдателей
            setupObservers()

            // Проверка разрешений для Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                checkForegroundServicePermission()
            }

            // Проверка разрешения на уведомления для Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkNotificationPermission()
            }

            // Проверка доступа к сервису доступности
            Log.d(TAG, "Проверка доступности сервиса...")
            checkAccessibilityServiceEnabled()

            // Проверка подключения внешних устройств
            checkExternalDevices()

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации MainActivity: ${e.message}", e)
            Toast.makeText(this, "Произошла ошибка при запуске приложения", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Проверяет разрешение на запуск foreground service в Android 14+
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC),
                    REQUEST_FOREGROUND_SERVICE_PERMISSION
                )
            }
        }
    }

    /**
     * Проверяет разрешение на отправку уведомлений в Android 13+
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission() {
        if (!permissionHelper.hasNotificationPermission()) {
            // Показываем диалог с объяснением, зачем нужно разрешение
            permissionHelper.showNotificationPermissionDialog(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_FOREGROUND_SERVICE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение получено
                    feedbackHelper.showToast(getString(R.string.service_started))
                } else {
                    // Разрешение не получено, показываем сообщение
                    feedbackHelper.showToast(getString(R.string.foreground_service_permission_required))
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на уведомления получено
                    feedbackHelper.showToast(getString(R.string.service_started))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Обновляем список профилей
        profileViewModel.loadProfiles()
    }

    override fun onResume() {
        super.onResume()
        
        // Обновляем статус сервиса при возвращении к активности
        updateServiceStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем текущее состояние
        outState.putString(KEY_SELECTED_PROFILE, lastSelectedProfileId)
        Log.d(TAG, getString(R.string.log_saved_state, lastSelectedProfileId ?: "null"))
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, getString(R.string.log_config_changed, newConfig.orientation))
        
        // Проверяем, не завершается ли активность
        if (isFinishing) return
        
        try {
            // Пересоздаем список при изменении конфигурации
            setupUI()
            
            // Обновляем отображение профилей
            profileViewModel.profiles.value?.let { updateProfileList(it) }
            
            // Обновляем статус сервиса
            updateServiceStatus()
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.config_update_error, e.message), e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_edit_mapping -> {
                openEditMapping()
                true
            }
            R.id.action_gamepad_config -> {
                openGamepadConfig()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
    // Настройка RecyclerView
        profileList = findViewById(R.id.profileList)
        profileList.layoutManager = LinearLayoutManager(this)

    // Добавляем анимацию
        val layoutAnimation = android.view.animation.LayoutAnimationController(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
        )
        layoutAnimation.delay = 0.15f
        layoutAnimation.order = android.view.animation.LayoutAnimationController.ORDER_NORMAL
        profileList.layoutAnimation = layoutAnimation

        // Настройка адаптера
        profileAdapter = ProfileAdapter(emptyList()) { profile, action ->
            when (action) {
                ProfileAction.ACTIVATE -> activateProfile(profile)
                ProfileAction.EDIT -> editProfile(profile)
                ProfileAction.DELETE -> confirmDeleteProfile(profile)
            }
        }
        profileList.adapter = profileAdapter

        // Настройка пустого представления
        emptyView = findViewById(R.id.emptyView)

        // Настройка FAB для добавления профиля
        addProfileButton = findViewById(R.id.addProfileButton)
        addProfileButton.setOnClickListener {
            showCreateProfileDialog()
        }

        // Настройка кнопки запуска/остановки сервиса
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        toggleServiceButton.setOnClickListener {
            toggleService()
        }
    }

    private fun setupObservers() {
        // Наблюдаем за списком профилей
        profileViewModel.profiles.observe(this) { profiles ->
            updateProfileList(profiles)
        }

        // Наблюдаем за активным профилем
        profileViewModel.activeProfile.observe(this) { profile ->
            // Можно обновить UI, чтобы показать активный профиль
        }

        // Наблюдаем за ошибками
        profileViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                feedbackHelper.showToast(it)
                profileViewModel.resetError()
            }
        }

        // Наблюдаем за состоянием загрузки
        profileViewModel.isLoading.observe(this) { isLoading ->
            // Можно показать/скрыть индикатор загрузки
        }
    }

    /**
     * Обновляет UI при изменении списка профилей с использованием DiffUtil
     * для эффективного обновления RecyclerView
     */
    private fun updateProfileList(profiles: List<GameProfile>) {
        try {
            val oldProfiles = profileAdapter.getProfiles()
            val diffCallback = ProfileDiffCallback(oldProfiles, profiles)
            val diffResult = DiffUtil.calculateDiff(diffCallback, true) // Используем detectMoves = true

            profileAdapter.updateProfiles(profiles)
            diffResult.dispatchUpdatesTo(profileAdapter)

            // Показываем пустое представление, если нет профилей
            if (profiles.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                profileList.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                profileList.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении списка профилей: ${e.message}", e)
        }
    }

    private fun showCreateProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_profile, null)
        val nameInput = dialogView.findViewById<TextView>(R.id.profileNameInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.create_profile)
            .setView(dialogView)
            .setPositiveButton(R.string.create) { _, _ ->
                val profileName = nameInput.text.toString().trim()
                if (profileName.isNotEmpty()) {
                    profileViewModel.createProfile(profileName)
                    feedbackHelper.showToast(getString(R.string.profile_created, profileName))
                } else {
                    feedbackHelper.showToast(getString(R.string.profile_name_empty))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun activateProfile(profile: GameProfile) {
        // Сохраняем ID активного профиля
        profileViewModel.activateProfile(profile.id)
        
        // Запоминаем ID выбранного профиля
        lastSelectedProfileId = profile.id

        // Проверяем, запущен ли сервис
        if (permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
            val intent = Intent(this, MappingService::class.java).apply {
                action = "LOAD_PROFILE"
                putExtra("profile_id", profile.id)
            }
            startService(intent)

            feedbackHelper.showToast(
                getString(R.string.profile_activated, profile.name)
            )
        } else {
            // Если сервис не запущен, предлагаем запустить его
            showServiceNotRunningDialog()
        }
    }

    private fun editProfile(profile: GameProfile) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val nameInput = dialogView.findViewById<TextView>(R.id.profileNameInput)
        val packageInput = dialogView.findViewById<TextView>(R.id.packageNameInput)

        nameInput.text = profile.name
        packageInput.text = profile.packageName

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_profile)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newPackage = packageInput.text.toString().trim()

                if (newName.isNotEmpty()) {
                    profile.name = newName
                    profile.packageName = newPackage
                    profileViewModel.updateProfile(profile)
                    feedbackHelper.showToast(getString(R.string.profile_updated))
                } else {
                    feedbackHelper.showToast(getString(R.string.profile_name_empty))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteProfile(profile: GameProfile) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.confirm_delete_message, profile.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                profileViewModel.deleteProfile(profile.id)
                feedbackHelper.showToast(getString(R.string.profile_deleted, profile.name))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openSettings() {
        // Открываем экран настроек
        feedbackHelper.showToast(getString(R.string.settings_not_implemented))
    }

    private fun openEditMapping() {
        val intent = Intent(this, EditMappingActivity::class.java)
        startActivity(intent)

        // Добавляем анимацию перехода
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openGamepadConfig() {
        val intent = Intent(this, GamepadConfigActivity::class.java)
        startActivity(intent)

        // Добавляем анимацию перехода
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun checkAccessibilityServiceEnabled() {
        if (!permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
            permissionHelper.showAccessibilityServiceDialog(this)
        }
    }

    private fun checkExternalDevices() {
        // Проверяем подключение внешних устройств
        val hasKeyboard = externalDeviceDetector.isKeyboardConnected()
        val hasMouse = externalDeviceDetector.isMouseConnected()
        val hasGamepad = externalDeviceDetector.isGamepadConnected()

        if (!hasKeyboard && !hasMouse && !hasGamepad) {
            // Если нет внешних устройств, показываем предупреждение
            AlertDialog.Builder(this)
                .setTitle(R.string.no_external_devices)
                .setMessage(R.string.no_external_devices_message)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun toggleService() {
        if (permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
            // Если сервис запущен, останавливаем его
            val intent = Intent(this, MappingService::class.java).apply {
                action = "STOP_SERVICE"
            }
            startService(intent)
        } else {
            // Если сервис не запущен, показываем диалог настройки доступности
            permissionHelper.showAccessibilityServiceDialog(this)
        }
    }

    private fun updateServiceStatus() {
        if (permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
            toggleServiceButton.setText(R.string.stop_service)
        } else {
            toggleServiceButton.setText(R.string.start_service)
        }
    }

    private fun showServiceNotRunningDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.service_not_running)
            .setMessage(R.string.service_not_running_message)
            .setPositiveButton(R.string.start_service) { _, _ ->
                toggleService()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    enum class ProfileAction {
        ACTIVATE, EDIT, DELETE
    }

    inner class ProfileAdapter(
        private var profiles: List<GameProfile>,
        private val onProfileAction: (GameProfile, ProfileAction) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {
    
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val profileName: TextView = view.findViewById(R.id.profileName)
            val packageName: TextView = view.findViewById(R.id.packageName)
            val activateButton: Button = view.findViewById(R.id.activateButton)
            val editButton: Button = view.findViewById(R.id.editButton)
            val deleteButton: Button = view.findViewById(R.id.deleteButton)
        }
    
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
            return ViewHolder(view)
        }
    
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val profile = profiles[position]
    
            holder.profileName.text = profile.name
            holder.packageName.text = if (profile.packageName.isNotEmpty()) {
                profile.packageName
            } else {
                getString(R.string.no_package_assigned)
            }
    
            holder.activateButton.setOnClickListener {
                onProfileAction(profile, ProfileAction.ACTIVATE)
            }
    
            holder.editButton.setOnClickListener {
                onProfileAction(profile, ProfileAction.EDIT)
            }
    
            holder.deleteButton.setOnClickListener {
                onProfileAction(profile, ProfileAction.DELETE)
            }
        }
    
        override fun getItemCount() = profiles.size
    
        fun getProfiles(): List<GameProfile> = profiles
    
        fun updateProfiles(newProfiles: List<GameProfile>) {
            profiles = newProfiles
        }
    }
}