package com.example.gamemapper

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider

/**
 * Диалог для настройки внешнего вида кнопок оверлея с живым предпросмотром
 */
class ButtonSettingsDialog(
    context: Context,
    private val initialSettings: ButtonSettings,
    private val onSettingsApplied: (ButtonSettings) -> Unit
) : Dialog(context) {

    // Настройки кнопки
    private val settings = initialSettings.copy()
    
    // Компоненты UI
    private lateinit var previewContainer: FrameLayout
    private lateinit var previewButton: CustomOverlayButton
    
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var shapeRecyclerView: RecyclerView
    private lateinit var colorRecyclerView: RecyclerView
    private lateinit var borderWidthSeekBar: SeekBar
    private lateinit var shadowSeekBar: SeekBar
    private lateinit var gradientSwitch: android.widget.Switch
    
    // Цвета и формы для выбора
    private val colorOptions = listOf(
        ColorOption("#3F51B5", "Indigo"),
        ColorOption("#2196F3", "Blue"),
        ColorOption("#4CAF50", "Green"),
        ColorOption("#FFC107", "Yellow"),
        ColorOption("#FF5722", "Orange"),
        ColorOption("#E91E63", "Pink"),
        ColorOption("#9C27B0", "Purple"),
        ColorOption("#FFFFFF", "White"),
        ColorOption("#000000", "Black")
    )
    
    private val shapeOptions = listOf(
        ShapeOption(CustomOverlayButton.SHAPE_CIRCLE, "Circle"),
        ShapeOption(CustomOverlayButton.SHAPE_PILL, "Pill"),
        ShapeOption(CustomOverlayButton.SHAPE_ROUNDED, "Rounded"),
        ShapeOption(CustomOverlayButton.SHAPE_SQUARE, "Square"),
        ShapeOption(CustomOverlayButton.SHAPE_DIAMOND, "Diamond"),
        ShapeOption(CustomOverlayButton.SHAPE_HEXAGON, "Hexagon")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_button_settings)
        
        // Инициализация компонентов UI
        previewContainer = findViewById(R.id.previewContainer)
        sizeSeekBar = findViewById(R.id.sizeSeekBar)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        shapeRecyclerView = findViewById(R.id.shapeRecyclerView)
        colorRecyclerView = findViewById(R.id.colorRecyclerView)
        borderWidthSeekBar = findViewById(R.id.borderWidthSeekBar)
        shadowSeekBar = findViewById(R.id.shadowSeekBar)
        gradientSwitch = findViewById(R.id.gradientSwitch)
        
        // Поле для ввода текста
        val buttonTextField = findViewById<android.widget.EditText>(R.id.buttonTextField)
        
        // Настройка заголовков
        findViewById<TextView>(R.id.sizeLabel).text = context.getString(R.string.button_size)
        findViewById<TextView>(R.id.opacityLabel).text = context.getString(R.string.button_opacity)
        findViewById<TextView>(R.id.shapeLabel).text = context.getString(R.string.button_shape)
        findViewById<TextView>(R.id.colorLabel).text = context.getString(R.string.button_color)
        findViewById<TextView>(R.id.borderLabel).text = context.getString(R.string.button_border)
        findViewById<TextView>(R.id.shadowLabel).text = context.getString(R.string.button_shadow)
        findViewById<TextView>(R.id.gradientLabel).text = context.getString(R.string.button_gradient)
        
        // Настройка кнопок
        val applyButton = findViewById<Button>(R.id.applyButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        
        applyButton.text = context.getString(R.string.apply)
        cancelButton.text = context.getString(R.string.cancel)
        
        // Настройка предпросмотра
        setupPreviewButton()
        
        // Настройка списков выбора
        setupRecyclerViews()
        
        // Настройка seekBar для размера
        sizeSeekBar.max = 200
        sizeSeekBar.progress = settings.buttonSize.toInt() - 50 // от 50 до 250
        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.buttonSize = (progress + 50).toFloat()
                updatePreviewButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Настройка seekBar для прозрачности
        opacitySeekBar.max = 255
        opacitySeekBar.progress = settings.buttonAlpha
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.buttonAlpha = progress
                updatePreviewButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Настройка seekBar для ширины границы
        borderWidthSeekBar.max = 10
        borderWidthSeekBar.progress = settings.buttonBorderWidth.toInt()
        borderWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.buttonBorderWidth = progress.toFloat()
                updatePreviewButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Настройка seekBar для тени
        shadowSeekBar.max = 20
        shadowSeekBar.progress = settings.buttonShadowRadius.toInt()
        shadowSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.buttonShadowRadius = progress.toFloat()
                updatePreviewButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Настройка переключателя градиента
        gradientSwitch.isChecked = settings.buttonGradient
        gradientSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.buttonGradient = isChecked
            updatePreviewButton()
        }
        
        // Инициализация текстового поля
        if (buttonTextField != null) {
            buttonTextField.setText(settings.buttonText)
            buttonTextField.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    settings.buttonText = s.toString()
                    updatePreviewButton()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        
        // Настройка кнопок
        applyButton.setOnClickListener {
            onSettingsApplied(settings)
            dismiss()
        }
        
        cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupPreviewButton() {
        previewButton = CustomOverlayButton(context).apply {
            buttonSize = settings.buttonSize
            buttonColor = settings.buttonColor
            buttonShape = settings.buttonShape
            buttonText = settings.buttonText.takeIf { it.isNotEmpty() } ?: "A"
            buttonTextColor = settings.buttonTextColor
            buttonBorderColor = settings.buttonBorderColor
            buttonBorderWidth = settings.buttonBorderWidth
            buttonAlpha = settings.buttonAlpha
            buttonGradient = settings.buttonGradient
            buttonShadowRadius = settings.buttonShadowRadius
        }
        
        previewContainer.removeAllViews()
        previewContainer.addView(previewButton)
    }
    
    private fun updatePreviewButton() {
        previewButton.buttonSize = settings.buttonSize
        previewButton.buttonColor = settings.buttonColor
        previewButton.buttonShape = settings.buttonShape
        previewButton.buttonTextColor = settings.buttonTextColor
        previewButton.buttonBorderColor = settings.buttonBorderColor
        previewButton.buttonBorderWidth = settings.buttonBorderWidth
        previewButton.buttonAlpha = settings.buttonAlpha
        previewButton.buttonGradient = settings.buttonGradient
        previewButton.buttonShadowRadius = settings.buttonShadowRadius
        
        // Обновляем текст, если он задан
        if (settings.buttonText.isNotEmpty()) {
            previewButton.buttonText = settings.buttonText
        }
        
        // Обновляем layout параметры для нового размера
        val params = previewButton.layoutParams
        params.width = settings.buttonSize.toInt()
        params.height = settings.buttonSize.toInt()
        previewButton.layoutParams = params
    }
    
    private fun setupRecyclerViews() {
        // Настройка RecyclerView для форм
        shapeRecyclerView.layoutManager = GridLayoutManager(context, 3)
        shapeRecyclerView.adapter = ShapeAdapter(shapeOptions) { selectedShape ->
            settings.buttonShape = selectedShape
            updatePreviewButton()
        }
        
        // Настройка RecyclerView для цветов
        colorRecyclerView.layoutManager = GridLayoutManager(context, 3)
        colorRecyclerView.adapter = ColorAdapter(colorOptions) { selectedColor ->
            settings.buttonColor = Color.parseColor(selectedColor)
            settings.buttonBorderColor = 
                if (isColorDark(selectedColor)) Color.WHITE else Color.BLACK
            settings.buttonTextColor = 
                if (isColorDark(selectedColor)) Color.WHITE else Color.BLACK
            updatePreviewButton()
        }
    }
    
    private fun isColorDark(colorHex: String): Boolean {
        val color = Color.parseColor(colorHex)
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
    
    /**
     * Адаптер для выбора формы кнопки
     */
    inner class ShapeAdapter(
        private val shapes: List<ShapeOption>,
        private val onShapeSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ShapeAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val shapeView: CustomOverlayButton = view.findViewById(R.id.shapePreview)
            val shapeText: TextView = view.findViewById(R.id.shapeName)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shape, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val shape = shapes[position]
            
            holder.shapeView.buttonShape = shape.shapeId
            holder.shapeView.buttonText = ""
            holder.shapeView.buttonColor = if (settings.buttonShape == shape.shapeId) 
                Color.parseColor("#3F51B5") else Color.LTGRAY
            
            holder.shapeText.text = shape.shapeName
            
            holder.itemView.setOnClickListener {
                onShapeSelected(shape.shapeId)
                notifyDataSetChanged() // Обновляем выделение
            }
        }
        
        override fun getItemCount() = shapes.size
    }
    
    /**
     * Адаптер для выбора цвета кнопки
     */
    inner class ColorAdapter(
        private val colors: List<ColorOption>,
        private val onColorSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorView: View = view.findViewById(R.id.colorPreview)
            val colorText: TextView = view.findViewById(R.id.colorName)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val colorOption = colors[position]
            
            val colorDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor(colorOption.colorHex))
            }
            
            holder.colorView.background = colorDrawable
            holder.colorText.text = colorOption.colorName
            
            // Выделяем выбранный цвет
            val isSelected = Color.parseColor(colorOption.colorHex) == settings.buttonColor
            holder.itemView.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(context, R.color.colorAccent_15)
                else Color.TRANSPARENT
            )
            
            holder.itemView.setOnClickListener {
                onColorSelected(colorOption.colorHex)
                notifyDataSetChanged() // Обновляем выделение
            }
        }
        
        override fun getItemCount() = colors.size
    }
    
    /**
     * Настройки кнопки для передачи
     */
    data class ButtonSettings(
        var buttonSize: Float = 120f,
        var buttonColor: Int = Color.parseColor("#3F51B5"),
        var buttonShape: Int = CustomOverlayButton.SHAPE_PILL,
        var buttonTextColor: Int = Color.WHITE,
        var buttonBorderColor: Int = Color.WHITE,
        var buttonBorderWidth: Float = 2f,
        var buttonAlpha: Int = 180,
        var buttonGradient: Boolean = true,
        var buttonShadowRadius: Float = 4f,
        var buttonText: String = ""
    )
    
    data class ShapeOption(val shapeId: Int, val shapeName: String)
    data class ColorOption(val colorHex: String, val colorName: String)
}
