package com.example.gamemapper

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.gamemapper.GestureType

/**
 * Класс, представляющий маппинг жеста на экране
 */
@Parcelize
data class GestureMapping(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: GestureType = GestureType.TAP,
    val x: Float = 0f,
    val y: Float = 0f,
    val keyCode: Int = 0
) : Parcelable
