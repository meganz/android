package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.contacts.group.data.ContactGroupUser

/**
 * Meeting item
 *
 * @property chatId             Chat id
 * @property id                 Scheduled meeting id
 * @property title              Scheduled meeting title
 * @property description        Scheduled meeting description
 * @property date               Scheduled meeting date
 * @property isHost             Check if user has host permissions
 * @property firstUser          First user of the chat
 * @property lastUser           Last user of the chat
 */
data class ScheduledMeetingItem constructor(
    val chatId: Long,
    val id: Long,
    val title: String,
    val description: String?,
    val date: String?,
    val isHost: Boolean,
    val firstUser: ContactGroupUser,
    val lastUser: ContactGroupUser?,
) {

    /**
     * Check if meeting contains only 1 user and myself
     *
     * @return  true if its single false otherwise
     */
    fun isSingleMeeting(): Boolean =
        lastUser == null

    class DiffCallback : DiffUtil.ItemCallback<ScheduledMeetingItem>() {
        override fun areItemsTheSame(oldItem: ScheduledMeetingItem, newItem: ScheduledMeetingItem): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: ScheduledMeetingItem, newItem: ScheduledMeetingItem): Boolean =
            oldItem == newItem
    }
}
