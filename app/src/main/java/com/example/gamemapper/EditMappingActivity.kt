package com.example.gamemapper

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_mapping)

        // Инициализация зависимостей
        mappingRepository = AppModule.getMappingRepository()
        feedbackHelper = AppModule.getFeedbackHelper()

        val keyList = findViewById<RecyclerView>(R.id.keyList)
        val addButton = findViewById<Button>(R.id.addButton)

        // Инициализация адаптера с данными из репозитория
        val mappings = mappingRepository.getKeyMappings().toMutableMap()
        setupAdapter(mappings)

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
            feedbackHelper.showToast(getString(R.string.new_mapping_added))
        }
    }

    private fun setupAdapter(mappings: MutableMap<Int, Pair<Float, Float>>) {
        adapter = KeyMappingAdapter(mappings) { keyCode, x, y ->
            mappings[keyCode] = Pair(x, y)
            saveOverlayPositions(mappings)
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

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        // Добавляем анимацию при закрытии активности
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}