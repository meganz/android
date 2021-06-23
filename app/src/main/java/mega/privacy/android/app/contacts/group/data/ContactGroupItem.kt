package mega.privacy.android.app.contacts.group.data

import androidx.recyclerview.widget.DiffUtil

data class ContactGroupItem constructor(
    val chatId: Long,
    val title: String,
    val firstUser: ContactGroupUser,
    val lastUser: ContactGroupUser,
    val isPublic: Boolean
) {

    class DiffCallback : DiffUtil.ItemCallback<ContactGroupItem>() {

        override fun areItemsTheSame(oldItem: ContactGroupItem, newItem: ContactGroupItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: ContactGroupItem, newItem: ContactGroupItem): Boolean =
            oldItem == newItem
    }
}
