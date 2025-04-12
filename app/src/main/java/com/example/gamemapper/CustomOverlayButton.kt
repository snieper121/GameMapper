package com.example.gamemapper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * Кастомная кнопка-оверлей с различными формами и настройками
 */
class CustomOverlayButton private constructor(
    context: Context,
    private val builder: Builder
) : View(context) {

    // Свойства кнопки
    var buttonSize: Float = builder.buttonSize
        set(value) {
            field = value
            requestLayout()
        }

    var buttonColor: Int = builder.buttonColor
        set(value) {
            field = value
            invalidate()
        }

    var buttonShape: Int = builder.buttonShape
        set(value) {
            field = value
            invalidate()
        }

    var buttonText: String = builder.buttonText
        set(value) {
            field = value
            invalidate()
        }

    var buttonTextColor: Int = builder.buttonTextColor
        set(value) {
            field = value
            textPaint.color = value
            invalidate()
        }

    var buttonBorderColor: Int = builder.buttonBorderColor
        set(value) {
            field = value
            borderPaint.color = value
            invalidate()
        }

    var buttonBorderWidth: Float = builder.buttonBorderWidth
        set(value) {
            field = value
            borderPaint.strokeWidth = value
            invalidate()
        }

    var buttonAlpha: Int = builder.buttonAlpha
        set(value) {
            field = value
            invalidate()
        }

    // Кисти для рисования
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    companion object {
        const val SHAPE_CIRCLE = 0
        const val SHAPE_SQUARE = 1
        const val SHAPE_ROUNDED = 2
        const val SHAPE_TRIANGLE = 3
        const val SHAPE_DIAMOND = 4
    }

    init {
        // Настройка кистей
        textPaint.color = buttonTextColor
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = buttonBorderColor
        borderPaint.strokeWidth = buttonBorderWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Устанавливаем прозрачность
        paint.alpha = buttonAlpha
        paint.color = buttonColor

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2f - 2f

        // Рисуем форму кнопки
        when (buttonShape) {
            SHAPE_CIRCLE -> {
                canvas.drawCircle(centerX, centerY, radius, paint)
                if (buttonBorderWidth > 0) {
                    canvas.drawCircle(centerX, centerY, radius, borderPaint)
                }
            }
            SHAPE_SQUARE -> {
                rect.set(2f, 2f, width - 2f, height - 2f)
                canvas.drawRect(rect, paint)
                if (buttonBorderWidth > 0) {
                    canvas.drawRect(rect, borderPaint)
                }
            }
            SHAPE_ROUNDED -> {
                rect.set(2f, 2f, width - 2f, height - 2f)
                canvas.drawRoundRect(rect, 15f, 15f, paint)
                if (buttonBorderWidth > 0) {
                    canvas.drawRoundRect(rect, 15f, 15f, borderPaint)
                }
            }
            SHAPE_TRIANGLE -> {
                val path = Path()
                path.moveTo(centerX, 2f)
                path.lineTo(width - 2f, height - 2f)
                path.lineTo(2f, height - 2f)
                path.close()
                canvas.drawPath(path, paint)
                if (buttonBorderWidth > 0) {
                    canvas.drawPath(path, borderPaint)
                }
            }
            SHAPE_DIAMOND -> {
                val path = Path()
                path.moveTo(centerX, 2f)
                path.lineTo(width - 2f, centerY)
                path.lineTo(centerX, height - 2f)
                path.lineTo(2f, centerY)
                path.close()
                canvas.drawPath(path, paint)
                if (buttonBorderWidth > 0) {
                    canvas.drawPath(path, borderPaint)
                }
            }
        }

        // Рисуем текст в центре кнопки
        textPaint.color = buttonTextColor
        val textHeight = textPaint.descent() - textPaint.ascent()
        val textOffset = textHeight / 2 - textPaint.descent()
        canvas.drawText(buttonText, centerX, centerY + textOffset, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = buttonSize.toInt()
        setMeasuredDimension(size, size)
    }

    /**
     * Builder для создания кнопки с настраиваемыми параметрами
     */
    class Builder(private val context: Context) {
        var buttonSize: Float = 120f
        var buttonColor: Int = Color.parseColor("#80FFFFFF")
        var buttonShape: Int = SHAPE_CIRCLE
        var buttonText: String = ""
        var buttonTextColor: Int = Color.BLACK
        var buttonBorderColor: Int = Color.BLACK
        var buttonBorderWidth: Float = 0f
        var buttonAlpha: Int = 128

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

        fun build(): CustomOverlayButton {
            return CustomOverlayButton(context, this)
        }
    }

    /**
     * Фабричный метод для создания кнопки
     */
    constructor(context: Context) : this(context, Builder(context))
}
