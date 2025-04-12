package com.example.gamemapper

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import com.example.gamemapper.di.AppModule

/**
 * Диалог для настройки параметров кнопки оверлея
 */
class ButtonSettingsDialog(
    private val context: Context,
    private val button: CustomOverlayButton,
    private val onSettingsChanged: (CustomOverlayButton) -> Unit
) {
    private lateinit var dialog: AlertDialog
    private lateinit var colorPreview: View
    private lateinit var sizeValueText: TextView
    private lateinit var transparencyValueText: TextView

    private val errorHandler = AppModule.getErrorHandler()

    /**
     * Показывает диалог настройки кнопки
     */
    fun show() {
        try {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_button_settings, null)

            // Инициализация элементов управления
            val shapeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.shapeRadioGroup)
            val shapeCircle = dialogView.findViewById<RadioButton>(R.id.shapeCircle)
            val shapeSquare = dialogView.findViewById<RadioButton>(R.id.shapeSquare)
            val shapeRounded = dialogView.findViewById<RadioButton>(R.id.shapeRounded)
            val shapeTriangle = dialogView.findViewById<RadioButton>(R.id.shapeTriangle)
            val shapeDiamond = dialogView.findViewById<RadioButton>(R.id.shapeDiamond)

            colorPreview = dialogView.findViewById(R.id.colorPreview)
            val selectColorButton = dialogView.findViewById<Button>(R.id.selectColorButton)
            val sizeSeekBar = dialogView.findViewById<SeekBar>(R.id.sizeSeekBar)
            sizeValueText = dialogView.findViewById(R.id.sizeValueText)
            val transparencySeekBar = dialogView.findViewById<SeekBar>(R.id.transparencySeekBar)
            transparencyValueText = dialogView.findViewById(R.id.transparencyValueText)

            // Установка текущих значений
            when (button.buttonShape) {
                CustomOverlayButton.SHAPE_CIRCLE -> shapeCircle.isChecked = true
                CustomOverlayButton.SHAPE_SQUARE -> shapeSquare.isChecked = true
                CustomOverlayButton.SHAPE_ROUNDED -> shapeRounded.isChecked = true
                CustomOverlayButton.SHAPE_TRIANGLE -> shapeTriangle.isChecked = true
                CustomOverlayButton.SHAPE_DIAMOND -> shapeDiamond.isChecked = true
            }

            colorPreview.setBackgroundColor(button.buttonColor)
            sizeSeekBar.progress = button.buttonSize.toInt()
            sizeValueText.text = button.buttonSize.toInt().toString()
            transparencySeekBar.progress = button.buttonAlpha
            updateTransparencyText(button.buttonAlpha)

            // Обработчики событий
            shapeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.shapeCircle -> button.buttonShape = CustomOverlayButton.SHAPE_CIRCLE
                    R.id.shapeSquare -> button.buttonShape = CustomOverlayButton.SHAPE_SQUARE
                    R.id.shapeRounded -> button.buttonShape = CustomOverlayButton.SHAPE_ROUNDED
                    R.id.shapeTriangle -> button.buttonShape = CustomOverlayButton.SHAPE_TRIANGLE
                    R.id.shapeDiamond -> button.buttonShape = CustomOverlayButton.SHAPE_DIAMOND
                }
                button.invalidate()
                onSettingsChanged(button)
            }

            selectColorButton.setOnClickListener {
                showColorPickerDialog()
            }

            sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val size = maxOf(progress, 60) // Минимальный размер 60
                    sizeValueText.text = size.toString()
                    button.buttonSize = size.toFloat()
                    button.requestLayout()
                    onSettingsChanged(button)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    button.buttonAlpha = progress
                    updateTransparencyText(progress)
                    button.invalidate()
                    onSettingsChanged(button)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            // Создание диалога
            dialog = AlertDialog.Builder(context)
                .setTitle(R.string.button_settings)
                .setView(dialogView)
                .setPositiveButton(R.string.ok) { _, _ -> }
                .create()

            dialog.show()
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось открыть диалог настроек кнопки")
        }
    }

    /**
     * Обновляет текст прозрачности
     */
    private fun updateTransparencyText(alpha: Int) {
        val percent = (alpha * 100 / 255)
        transparencyValueText.text = "$percent%"
    }

    /**
     * Показывает диалог выбора цвета
     */
    private fun showColorPickerDialog() {
        try {
            val colors = arrayOf(
                Color.parseColor("#80FFFFFF"), // Полупрозрачный белый
                Color.parseColor("#80FF0000"), // Полупрозрачный красный
                Color.parseColor("#8000FF00"), // Полупрозрачный зеленый
                Color.parseColor("#800000FF"), // Полупрозрачный синий
                Color.parseColor("#80FFFF00"), // Полупрозрачный желтый
                Color.parseColor("#80FF00FF"), // Полупрозрачный пурпурный
                Color.parseColor("#8000FFFF"), // Полупрозрачный голубой
                Color.parseColor("#80000000")  // Полупрозрачный черный
            )

            val colorNames = arrayOf(
                context.getString(R.string.color_white),
                context.getString(R.string.color_red),
                context.getString(R.string.color_green),
                context.getString(R.string.color_blue),
                context.getString(R.string.color_yellow),
                context.getString(R.string.color_magenta),
                context.getString(R.string.color_cyan),
                context.getString(R.string.color_black)
            )

            showColorSelectionDialog(colors, colorNames)
        } catch (e: Exception) {
            errorHandler.handleError(e, "Не удалось открыть диалог выбора цвета")
        }
    }

    /**
     * Показывает диалог выбора цвета из предложенных вариантов
     */
    private fun showColorSelectionDialog(colors: Array<Int>, colorNames: Array<String>) {
        AlertDialog.Builder(context)
            .setTitle(R.string.button_color)
            .setItems(colorNames) { _, which ->
                button.buttonColor = colors[which]
                colorPreview.setBackgroundColor(colors[which])
                button.invalidate()
                onSettingsChanged(button)
            }
            .show()
    }
}
