package com.example.gamemapper.helpers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import android.view.View
import com.example.gamemapper.di.AppModule

/**
 * Помощник для предоставления обратной связи пользователю (тосты, вибрация и т.д.)
 */
class FeedbackHelper(private val context: Context) {

    private val vibrator by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации вибратора: ${e.message}", e)
            null
        }
    }

    /**
     * Показывает toast сообщение
     */
    fun showToast(message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при показе toast: ${e.message}", e)
        }
    }

    /**
     * Показывает toast сообщение из ресурсов
     */
    fun showToast(@StringRes messageResId: Int) {
        try {
            Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при показе toast из ресурса: ${e.message}", e)
        }
    }

    /**
     * Показать сообщение с возможностью действия
     */
    fun showSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }

    /**
     * Вибрирует устройство на указанное количество миллисекунд
     */
    fun vibrate(durationMs: Long) {
        try {
            val vibrator = vibrator ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                ))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при вибрации: ${e.message}", e)
        }
    }
    
    /**
     * Производит вибрацию в виде простой последовательности (например, для успешного результата)
     */
    fun vibrateSuccess() {
        try {
            val vibrator = vibrator ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 100, 50)
                val amplitudes = intArrayOf(0, 100, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 100, 50), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении вибрации: ${e.message}", e)
        }
    }
    
    /**
     * Производит вибрацию в виде предупреждения
     */
    fun vibrateWarning() {
        try {
            val vibrator = vibrator ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 100, 100)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении вибрации: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "FeedbackHelper"
    }
}
