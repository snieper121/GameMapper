package com.example.gamemapper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemapper.di.AppModule
import com.example.gamemapper.helpers.FeedbackHelper
import com.example.gamemapper.repository.interfaces.IMappingRepository

class EditMappingActivity : AppCompatActivity() {

    private lateinit var adapter: KeyMappingAdapter
    private lateinit var mappingRepository: IMappingRepository
    private lateinit var feedbackHelper: FeedbackHelper

    private var mappingService: MappingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MappingService.LocalBinder
            mappingService = binder.getService()
            bound = true

            // Инициализация адаптера с данными из сервиса
            val mappings = mappingService?.keyMappings?.toMutableMap() ?: mutableMapOf()
            setupAdapter(mappings)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mappingService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_mapping)

        // Инициализация зависимостей
        mappingRepository = AppModule.getMappingRepository()
        feedbackHelper = AppModule.getFeedbackHelper()

        val keyList = findViewById<RecyclerView>(R.id.keyList)
        val addButton = findViewById<Button>(R.id.addButton)

        // Привязка к сервису
        val intent = Intent(this, MappingService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Временно инициализируем адаптер с пустой картой
        // Будет обновлен после подключения к сервису
        setupAdapter(mutableMapOf())

        keyList.adapter = adapter
        keyList.layoutManager = LinearLayoutManager(this)

        // Добавляем анимацию
        val layoutAnimation = android.view.animation.LayoutAnimationController(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
        )
        layoutAnimation.delay = 0.15f
        layoutAnimation.order = android.view.animation.LayoutAnimationController.ORDER_NORMAL
        keyList.layoutAnimation = layoutAnimation

        addButton.setOnClickListener {
            val mappings = adapter.getMappings()
            mappings[KeyEvent.KEYCODE_UNKNOWN] = Pair(0f, 0f)
            adapter.notifyItemInserted(mappings.size - 1)
            saveOverlayPositions(mappings)
            updateServiceMappings(mappings)
            feedbackHelper.showToast(getString(R.string.new_mapping_added))
        }
    }

    private fun setupAdapter(mappings: MutableMap<Int, Pair<Float, Float>>) {
        adapter = KeyMappingAdapter(mappings) { keyCode, x, y ->
            mappings[keyCode] = Pair(x, y)
            saveOverlayPositions(mappings)
            updateServiceMappings(mappings)
        }

        // Проверяем, есть ли уже адаптер у RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.keyList)
        if (recyclerView.adapter == null) {
            recyclerView.adapter = adapter
        } else {
            // Обновляем существующий адаптер
            recyclerView.swapAdapter(adapter, true)
        }
    }

    private fun saveOverlayPositions(mappings: Map<Int, Pair<Float, Float>>) {
        // Сохраняем маппинги в репозиторий
        mappingRepository.updateKeyMappings(mappings)
    }

    private fun updateServiceMappings(mappings: Map<Int, Pair<Float, Float>>) {
        if (bound && mappingService != null) {
            mappingService?.updateKeyMappings(mappings)
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

