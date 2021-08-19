package mega.privacy.android.app.contacts.group.data

import androidx.recyclerview.widget.DiffUtil

/**
 * View item that represents a Contact Group at UI level.
 *
 * @property chatId     Contact group Id
 * @property title      Contact group Title
 * @property firstUser  First contact of the group
 * @property lastUser   Last contact of the group
 * @property isPublic   Flag to know if the group is public
 */
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
