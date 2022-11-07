package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants

/**
 * Meeting item
 *
 * @property chatId                     Chat id
 * @property id                         Scheduled meeting id
 * @property title                      Scheduled meeting title
 * @property description                Scheduled meeting description
 * @property date                       Scheduled meeting date
 * @property isHost                     Check if user has host permissions
 * @property isAllowAddParticipants     Check if it is allow for non-host add participants
 * @property firstUser                  First user of the chat
 * @property lastUser                   Last user of the chat
 */
data class ScheduledMeetingItem constructor(
    val chatId: Long = -1,
    val id: Long = -1,
    val title: String = "Photos Sprint #1",
    val description: String = "description",
    val date: String = "8 Aud 2022 · 10:00-11:00",
    val isHost: Boolean = true,
    val isAllowAddParticipants: Boolean = true,
    val firstUser: ContactGroupUser? = ContactGroupUser(-1,
        "firstuser@mega.nz",
        "A",
        null,
        AvatarUtil.getSpecificAvatarColor(
            Constants.AVATAR_PRIMARY_COLOR)),
    val lastUser: ContactGroupUser? = ContactGroupUser(-1,
        "lastUser@mega.nz",
        "B",
        null,
        AvatarUtil.getSpecificAvatarColor(
            Constants.AVATAR_PRIMARY_COLOR)),
) {

    /**
     * Check if the meeting does not contain participants
     *
     * @return  true if its empty, false otherwise
     */
    fun isEmptyMeeting(): Boolean = firstUser == null

    /**
     * Check if meeting contains only 1 user and myself
     *
     * @return  true if its single, false otherwise
     */
    fun isSingleMeeting(): Boolean =
        lastUser == null

    class DiffCallback : DiffUtil.ItemCallback<ScheduledMeetingItem>() {
        override fun areItemsTheSame(
            oldItem: ScheduledMeetingItem,
            newItem: ScheduledMeetingItem,
        ): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(
            oldItem: ScheduledMeetingItem,
            newItem: ScheduledMeetingItem,
        ): Boolean =
            oldItem == newItem
    }
}
