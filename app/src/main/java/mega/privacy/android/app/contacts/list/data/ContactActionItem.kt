package mega.privacy.android.app.contacts.list.data

import androidx.recyclerview.widget.DiffUtil

/**
 * View item that represents a Contact Action at UI level.
 * This can be either `REQUESTS` or `GROUPS`
 *
 * @property id         Action Id.
 * @property title      Action Title
 * @property leadingIcon Item Icon
 * @property counter    Notification counter
 */
data class ContactActionItem(
    val id: Type,
    val title: String,
    val leadingIcon: Int,
    val counter: Int = 0,
) {

    /**
     * Type
     */
    enum class Type {
        /**
         * Requests
         */
        REQUESTS,

        /**
         * Groups
         */
        GROUPS
    }

    /**
     * Diff callback
     */
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
