package com.example.gamemapper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var profileList: RecyclerView
    private lateinit var emptyView: TextView

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var externalDeviceDetector: ExternalDeviceDetector

    private var mappingService: MappingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MappingService.LocalBinder
            mappingService = binder.getService()
            bound = true

            // Обновляем UI, если сервис подключен
            updateServiceStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mappingService = null
            bound = false

            // Обновляем UI, если сервис отключен
            updateServiceStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация зависимостей
        permissionHelper = AppModule.getPermissionHelper()
        feedbackHelper = AppModule.getFeedbackHelper()
        externalDeviceDetector = AppModule.getExternalDeviceDetector()

        // Инициализация ViewModel
        profileViewModel = ViewModelProvider(
            this,
            AppModule.getProfileViewModelFactory()
        ).get(ProfileViewModel::class.java)

        // Настройка UI
        setupUI()

        // Настройка наблюдателей
        setupObservers()

        // Проверка доступа к сервису доступности
        checkAccessibilityServiceEnabled()

        // Проверка подключения внешних устройств
        checkExternalDevices()

        // Привязка к сервису
        val intent = Intent(this, MappingService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()

        // Обновляем список профилей
        profileViewModel.loadProfiles()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
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
        val addProfileButton = findViewById<FloatingActionButton>(R.id.addProfileButton)
        addProfileButton.setOnClickListener {
            showCreateProfileDialog()
        }

        // Настройка кнопки запуска/остановки сервиса
        val toggleServiceButton = findViewById<Button>(R.id.toggleServiceButton)
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

    private fun updateProfileList(profiles: List<GameProfile>) {
        val oldProfiles = profileAdapter.getProfiles()
        val diffCallback = ProfileDiffCallback(oldProfiles, profiles)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

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

        // Если сервис запущен, загружаем профиль
        if (bound && mappingService != null) {
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
        if (bound && mappingService != null &&
            permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
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
        val toggleServiceButton = findViewById<Button>(R.id.toggleServiceButton)

        if (bound && mappingService != null &&
            permissionHelper.isAccessibilityServiceEnabled(MappingService::class.java.name)) {
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

