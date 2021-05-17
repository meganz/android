package mega.privacy.android.app.contacts.group.data

import android.net.Uri
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil

data class GroupItem constructor(
    val chatId: Long,
    val title: String,
    val firstImageEmail: String,
    val firstImage: Uri? = null,
    @ColorInt val firstImageColor: Int,
    val secondImageEmail: String,
    val secondImage: Uri? = null,
    @ColorInt val secondImageColor: Int,
    val isPublic: Boolean
) {

    class DiffCallback : DiffUtil.ItemCallback<GroupItem>() {

        override fun areItemsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean =
            oldItem == newItem
    }
}
