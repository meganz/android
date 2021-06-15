package mega.privacy.android.app.contacts.list.data

import androidx.recyclerview.widget.DiffUtil

data class ContactActionItem(
    val id: Type,
    val title: String,
    val counter: Int = 0
) {

    enum class Type { REQUESTS, GROUPS }

    class DiffCallback : DiffUtil.ItemCallback<ContactActionItem>() {
        override fun areItemsTheSame(
            oldItem: ContactActionItem,
            newItem: ContactActionItem
        ): Boolean =
            oldItem.title == newItem.title

        override fun areContentsTheSame(
            oldItem: ContactActionItem,
            newItem: ContactActionItem
        ): Boolean =
            oldItem == newItem
    }
}
