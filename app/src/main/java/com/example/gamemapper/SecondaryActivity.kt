package com.example.gamemapper

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Упрощенная версия MainActivity для тестирования компиляции
 */
class SecondaryActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondary)
        
        // Инициализация базового UI
        textView = findViewById(R.id.textView)
        textView.text = "GameMapper запущен"
        
        button = findViewById(R.id.button)
        button.setOnClickListener {
            textView.text = "Кнопка нажата!"
        }
    }
}