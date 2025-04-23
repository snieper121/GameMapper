package com.example.gamemapper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

/**
 * Кастомная кнопка-оверлей с различными формами и настройками
 * Улучшенная версия с анимациями и эффектами как в Octopus
 */
class CustomOverlayButton private constructor(
    context: Context,
    private val builder: Builder
) : FrameLayout(context) {

    // Свойства кнопки
    var buttonSize: Float = builder.buttonSize
        set(value) {
            field = value
            requestLayout()
        }

    var buttonColor: Int = builder.buttonColor
        set(value) {
            field = value
            updateBackground()
        }

    var buttonShape: Int = builder.buttonShape
        set(value) {
            field = value
            updateBackground()
        }

    var buttonText: String = builder.buttonText
        set(value) {
            field = value
            updateTextView()
        }

    var buttonTextColor: Int = builder.buttonTextColor
        set(value) {
            field = value
            updateTextView()
        }

    var buttonBorderColor: Int = builder.buttonBorderColor
        set(value) {
            field = value
            updateBackground()
        }

    var buttonBorderWidth: Float = builder.buttonBorderWidth
        set(value) {
            field = value
            updateBackground()
        }

    var buttonAlpha: Int = builder.buttonAlpha
        set(value) {
            field = value
            this.alpha = value / 255f
        }

    // Аттрибуты для улучшенного внешнего вида
    var buttonGradient: Boolean = builder.buttonGradient
        set(value) {
            field = value
            updateBackground()
        }

    var buttonShadowRadius: Float = builder.buttonShadowRadius
        set(value) {
            field = value
            elevation = value
            invalidate()
        }

    // Вложенные view для текста и эффектов
    private val textView: android.widget.TextView
    private val rippleView: View
    
    // Анимации
    private val pressAnimation: ScaleAnimation
    private val releaseAnimation: ScaleAnimation

    companion object {
        const val SHAPE_CIRCLE = 0
        const val SHAPE_SQUARE = 1
        const val SHAPE_ROUNDED = 2
        const val SHAPE_TRIANGLE = 3
        const val SHAPE_DIAMOND = 4
        const val SHAPE_HEXAGON = 5
        const val SHAPE_PILL = 6
    }

    init {
        // Настраиваем базовые параметры
        clipChildren = true
        clipToPadding = true
        
        // Настраиваем анимации
        pressAnimation = ScaleAnimation(
            1.0f, 0.95f, 1.0f, 0.95f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 100
            fillAfter = true
        }
        
        releaseAnimation = ScaleAnimation(
            0.95f, 1.0f, 0.95f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 150
            fillAfter = true
        }
        
        // Добавляем эффект пульсации при нажатии
        rippleView = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)
            isClickable = false
            isFocusable = false
            alpha = 0f
        }
        addView(rippleView)
        
        // Добавляем TextView для текста
        textView = android.widget.TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(buttonTextColor)
            text = buttonText
            isSingleLine = true
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
        }
        addView(textView)
        
        // Обновляем визуальное отображение
        updateBackground()
        updateTextView()
        
        // Устанавливаем начальную прозрачность
        alpha = buttonAlpha / 255f
        
        // Настройка эффектов нажатия
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startAnimation(pressAnimation)
                    rippleView.alpha = 0.15f
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    startAnimation(releaseAnimation)
                    rippleView.alpha = 0f
                    performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateBackground() {
        val shape = when (buttonShape) {
            SHAPE_CIRCLE -> GradientDrawable.OVAL
            SHAPE_ROUNDED -> GradientDrawable.RECTANGLE
            SHAPE_PILL -> GradientDrawable.RECTANGLE
            else -> GradientDrawable.RECTANGLE
        }
        
        val drawable = GradientDrawable().apply {
            setShape(shape)
            
            // Устанавливаем градиент если нужно
            if (buttonGradient) {
                val lighterColor = lightenColor(buttonColor, 0.2f)
                val darkerColor = darkenColor(buttonColor, 0.2f)
                setColors(intArrayOf(lighterColor, buttonColor, darkerColor))
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = buttonSize / 1.5f
            } else {
                setColor(buttonColor)
            }
            
            // Устанавливаем бордер
            if (buttonBorderWidth > 0) {
                setStroke(buttonBorderWidth.toInt(), buttonBorderColor)
            }
            
            // Скругление для прямоугольных форм
            if (buttonShape == SHAPE_ROUNDED) {
                cornerRadius = 15f
            } else if (buttonShape == SHAPE_PILL) {
                cornerRadius = buttonSize / 2
            }
        }
        
        background = drawable
    }
    
    private fun updateTextView() {
        textView.text = buttonText
        textView.setTextColor(buttonTextColor)
    }
    
    // Цветовые утилиты
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * (1 - factor)).toInt()
        val g = (Color.green(color) * (1 - factor)).toInt()
        val b = (Color.blue(color) * (1 - factor)).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = buttonSize.toInt()
        setMeasuredDimension(size, size)
        
        // Измеряем дочерние view
        val childWidthSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        
        for (i in 0 until childCount) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
    }

    /**
     * Builder для создания кнопки с настраиваемыми параметрами
     */
    class Builder(private val context: Context) {
        var buttonSize: Float = 120f
        var buttonColor: Int = Color.parseColor("#80FFFFFF")
        var buttonShape: Int = SHAPE_ROUNDED
        var buttonText: String = ""
        var buttonTextColor: Int = Color.BLACK
        var buttonBorderColor: Int = Color.BLACK
        var buttonBorderWidth: Float = 2f
        var buttonAlpha: Int = 180
        var buttonGradient: Boolean = true
        var buttonShadowRadius: Float = 4f

        fun setSize(size: Float): Builder {
            buttonSize = size
            return this
        }

        fun setColor(color: Int): Builder {
            buttonColor = color
            return this
        }

        fun setShape(shape: Int): Builder {
            buttonShape = shape
            return this
        }

        fun setText(text: String): Builder {
            buttonText = text
            return this
        }

        fun setTextColor(color: Int): Builder {
            buttonTextColor = color
            return this
        }

        fun setBorderColor(color: Int): Builder {
            buttonBorderColor = color
            return this
        }

        fun setBorderWidth(width: Float): Builder {
            buttonBorderWidth = width
            return this
        }

        fun setAlpha(alpha: Int): Builder {
            buttonAlpha = alpha
            return this
        }
        
        fun setGradient(gradient: Boolean): Builder {
            buttonGradient = gradient
            return this
        }
        
        fun setShadowRadius(radius: Float): Builder {
            buttonShadowRadius = radius
            return this
        }

        fun build(): CustomOverlayButton {
            return CustomOverlayButton(context, this)
        }
    }

    /**
     * Фабричный метод для создания кнопки
     */
    constructor(context: Context) : this(context, Builder(context))
}
