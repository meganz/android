package mega.privacy.android.app.meeting.list

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.contacts.group.data.ContactGroupUser

data class MeetingItem constructor(
    val chatId: Long,
    val title: String,
    val lastMessage: String,
    val timeStamp: Long,
    val firstUser: ContactGroupUser,
    val lastUser: ContactGroupUser,
) {

    class DiffCallback : DiffUtil.ItemCallback<MeetingItem>() {
        override fun areItemsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: MeetingItem, newItem: MeetingItem): Boolean =
            oldItem == newItem
    }
}
