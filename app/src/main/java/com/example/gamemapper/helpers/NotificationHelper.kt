package com.example.gamemapper.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.gamemapper.MainActivity
import com.example.gamemapper.MappingService
import com.example.gamemapper.R

/**
 * Помощник для работы с уведомлениями
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "GameMapperChannel"
        private const val SERVICE_NOTIFICATION_ID = 1
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Создает канал уведомлений (для Android 8.0+)
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Создает уведомление для сервиса
     */
    fun createServiceNotification(): Notification {
        val stopIntent = Intent(context, MappingService::class.java).apply {
            action = "STOP_SERVICE"
        }

        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )

        val settingsIntent = Intent(context, MainActivity::class.java)
        val settingsPendingIntent = PendingIntent.getActivity(
            context, 0, settingsIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.service_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(settingsPendingIntent)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.stop_service),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Показывает уведомление
     */
    fun showNotification(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    /**
     * Отменяет уведомление
     */
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    /**
     * Отменяет все уведомления
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
