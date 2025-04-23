package com.example.gamemapper.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.gamemapper.R

/**
 * Помощник для работы с разрешениями
 */
class PermissionHelper(private val context: Context) {

    /**
     * Проверяет, есть ли разрешение на наложение поверх других приложений
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Проверяет, включен ли сервис доступности
     */
    fun isAccessibilityServiceEnabled(serviceName: String): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val serviceString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return serviceString?.contains("${context.packageName}/$serviceName") == true
        }

        return false
    }

    /**
     * Проверяет разрешение на отправку уведомлений
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Для Android версий до 13 разрешение не требуется
        }
    }

    /**
     * Запрашивает разрешение на отправку уведомлений
     */
    fun requestNotificationPermission(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * Показывает диалог с запросом разрешения на наложение поверх других приложений
     */
    fun showOverlayPermissionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.overlay_permission_required)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                activity.startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    /**
     * Показывает диалог с запросом на включение сервиса доступности
     */
    fun showAccessibilityServiceDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.accessibility_required)
            .setMessage(R.string.accessibility_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                activity.startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    /**
     * Показывает диалог с объяснением, зачем нужно разрешение на уведомления
     */
    fun showNotificationPermissionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.notification_permission_required)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                if (activity is FragmentActivity) {
                    requestNotificationPermission(activity)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }
}
