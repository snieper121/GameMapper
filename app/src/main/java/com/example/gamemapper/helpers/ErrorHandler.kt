package com.example.gamemapper.helpers

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.gamemapper.R

/**
 * Централизованная обработка ошибок
 */
class ErrorHandler(private val context: Context) {

    companion object {
        private const val TAG = "ErrorHandler"
    }

    /**
     * Обработка ошибки с выводом сообщения пользователю
     */
    fun handleError(e: Exception, userMessage: String? = null, logMessage: String? = null) {
        // Логирование ошибки
        Log.e(TAG, logMessage ?: userMessage ?: e.message ?: "Unknown error", e)

        // Вывод сообщения пользователю
        val message = userMessage ?: context.getString(R.string.error_occurred)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Обработка ошибки без вывода сообщения пользователю
     */
    fun logError(e: Exception, logMessage: String? = null) {
        Log.e(TAG, logMessage ?: e.message ?: "Unknown error", e)
    }

    /**
     * Обработка ошибки с возвратом значения по умолчанию
     */
    fun <T> handleErrorWithDefault(
        defaultValue: T,
        operation: () -> T,
        userMessage: String? = null,
        logMessage: String? = null
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            handleError(e, userMessage, logMessage)
            defaultValue
        }
    }
}
