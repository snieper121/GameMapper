package com.example.gamemapper.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.gamemapper.GameProfile

/**
 * DiffUtil.Callback для эффективного обновления списка профилей
 */
class ProfileDiffCallback(
    private val oldList: List<GameProfile>,
    private val newList: List<GameProfile>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.name == newItem.name &&
                oldItem.packageName == newItem.packageName &&
                oldItem.isActive == newItem.isActive
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        // Можно использовать для частичного обновления элементов
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
