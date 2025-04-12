package com.example.gamemapper.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
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
}
