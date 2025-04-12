package com.example.gamemapper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent

/**
 * Адаптер для отображения и управления кнопками геймпада
 */
class GamepadButtonAdapter(
    private val buttons: MutableMap<Int, Pair<Float, Float>>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<GamepadButtonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val buttonNameText: TextView = view.findViewById(R.id.buttonNameText)
        val positionText: TextView = view.findViewById(R.id.positionText)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gamepad_button, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keyCode = buttons.keys.elementAt(position)
        val (x, y) = buttons[keyCode]!!

        holder.buttonNameText.text = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
        holder.positionText.text = "X: $x, Y: $y"

        holder.deleteButton.setOnClickListener {
            onDeleteClick(keyCode)
        }
    }

    override fun getItemCount() = buttons.size
}
