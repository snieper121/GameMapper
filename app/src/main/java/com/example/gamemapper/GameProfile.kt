package com.example.gamemapper

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Модель данных для профиля игры
 */
@Parcelize
data class GameProfile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var packageName: String = "",
    var isActive: Boolean = false,
    val keyMappings: MutableMap<Int, Pair<Float, Float>> = mutableMapOf(),
    val gestureMappings: MutableList<GestureMapping> = mutableListOf()
) : Parcelable {

    /**
     * Создает копию профиля с новыми базовыми свойствами, но с теми же маппингами
     */
    fun copyWithMappings(
        id: String = this.id,
        name: String = this.name,
        packageName: String = this.packageName,
        isActive: Boolean = this.isActive
    ): GameProfile {
        val newProfile = GameProfile(
            id = id,
            name = name,
            packageName = packageName,
            isActive = isActive
        )

        // Копируем маппинги
        newProfile.keyMappings.putAll(this.keyMappings)
        newProfile.gestureMappings.addAll(this.gestureMappings)

        return newProfile
    }
}
