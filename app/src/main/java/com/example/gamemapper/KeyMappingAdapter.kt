package com.example.gamemapper

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemapper.di.AppModule

/**
 * Адаптер для отображения и редактирования маппингов клавиш
 */
class KeyMappingAdapter(
    private val mappings: MutableMap<Int, Pair<Float, Float>>,
    private val onPositionChanged: (Int, Float, Float) -> Unit
) : RecyclerView.Adapter<KeyMappingAdapter.ViewHolder>() {

    private val errorHandler = AppModule.getErrorHandler()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val keyText: TextView = view.findViewById(R.id.keyText)
        val xEdit: EditText = view.findViewById(R.id.xPositionEdit)
        val yEdit: EditText = view.findViewById(R.id.yPositionEdit)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_key_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keyCode = mappings.keys.elementAt(position)
        val (x, y) = mappings[keyCode]!!

        holder.keyText.text = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
        holder.xEdit.setText(x.toString())
        holder.yEdit.setText(y.toString())

        // Обработчик изменения X-координаты
        holder.xEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    s?.toString()?.toFloatOrNull()?.let { newX ->
                        onPositionChanged(keyCode, newX, y)
                    }
                } catch (e: Exception) {
                    errorHandler.logError(e, "Error converting X position to float")
                }
            }
        })

        // Обработчик изменения Y-координаты
        holder.yEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    s?.toString()?.toFloatOrNull()?.let { newY ->
                        onPositionChanged(keyCode, x, newY)
                    }
                } catch (e: Exception) {
                    errorHandler.logError(e, "Error converting Y position to float")
                }
            }
        })

        // Обработчик удаления маппинга
        holder.deleteButton.setOnClickListener {
            mappings.remove(keyCode)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, mappings.size)
        }
    }

    override fun getItemCount() = mappings.size

    fun getMappings(): MutableMap<Int, Pair<Float, Float>> = mappings
}
