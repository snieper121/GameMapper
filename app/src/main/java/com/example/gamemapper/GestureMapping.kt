package com.example.gamemapper

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Модель данных для маппинга жестов
 */
@Parcelize
data class GestureMapping(
    val id: String = UUID.randomUUID().toString(),
    val gestureType: GestureType,
    val keyCode: Int,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val duration: Long
) : Parcelable
