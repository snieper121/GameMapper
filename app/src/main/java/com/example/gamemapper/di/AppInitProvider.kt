package com.example.gamemapper.di

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.example.gamemapper.PersistenceManager

/**
 * ContentProvider для инициализации приложения
 * Запускается автоматически при старте приложения до любой Activity
 */
class AppInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false

        // Инициализация модуля приложения
        AppModule.initialize(context)

        // Инициализация PersistenceManager
        PersistenceManager.init(context)

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
