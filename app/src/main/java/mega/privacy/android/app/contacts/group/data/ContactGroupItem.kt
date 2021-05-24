package mega.privacy.android.app.contacts.group.data

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class ContactGroupItem constructor(
    val chatId: Long,
    val title: String,
    val firstUserEmail: String,
    val firstUserAvatar: Uri? = null,
    val firstUserPlaceholder: Drawable,
    val lastUserEmail: String,
    val lastUserAvatar: Uri? = null,
    val lastUserPlaceholder: Drawable,
    val isPublic: Boolean
) {

    class DiffCallback : DiffUtil.ItemCallback<ContactGroupItem>() {

        override fun areItemsTheSame(oldItem: ContactGroupItem, newItem: ContactGroupItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: ContactGroupItem, newItem: ContactGroupItem): Boolean =
            oldItem == newItem
    }
}
