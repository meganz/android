package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport
import mega.privacy.android.app.contacts.group.data.GroupChatParticipant
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatPermissions
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility

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
    val date: String = "8 Aug 2022 · 10:00-11:00",
    val isHost: Boolean = true,
    val isAllowAddParticipants: Boolean = true,
    val firstUser: GroupChatParticipant? = GroupChatParticipant(
        user = ContactItem(handle = -1,
            email = "first@mega.nz",
            ContactData(fullName = "First", alias = null, avatarUri = null),
            defaultAvatarColor = Constants.AVATAR_PRIMARY_COLOR,
            visibility = UserVisibility.Visible,
            timestamp = -1,
            areCredentialsVerified = false,
            status = UserStatus.Online,
            lastSeen = null),
        permissions = ChatPermissions.Host
    ),
    val lastUser: GroupChatParticipant? = GroupChatParticipant(
        user = ContactItem(handle = -1,
            email = "last@mega.nz",
            ContactData(fullName = "Last", alias = null, avatarUri = null),
            defaultAvatarColor = Constants.AVATAR_PRIMARY_COLOR,
            visibility = UserVisibility.Visible,
            timestamp = -1,
            areCredentialsVerified = false,
            status = UserStatus.Online,
            lastSeen = null),
        permissions = ChatPermissions.Host
    ),
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
