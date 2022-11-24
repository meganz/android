package mega.privacy.android.app.presentation.meeting.model

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Data class defining the state of [ScheduledMeetingInfoViewModel]
 *
 * @property buttons                                    List of available action buttons.
 * @property chatId                                     Chat id.
 * @property chatTitle                                  Chat title.
 * @property scheduledMeeting                           Current scheduled meeting item.
 * @property isHost                                     If participant has host permissions.
 * @property isOpenInvite                               If open invite option is enabled.
 * @property participantItemList                        List of [ContactItem].
 * @property seeMoreVisible                             True if see more option is visible, false otherwise.
 * @property enabledMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property enabledChatNotificationsOption             True if is enabled the chat notifications option, false otherwise.
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property error                                      String resource id for showing an error.
 * @property result                                     Handle of the new chat conversation.
 * @property firstParticipant                           First participant in the chat room.
 * @property lastParticipant                            Last participant in the chat room.
 */
data class ScheduledMeetingInfoState(
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val chatId: Long = -1,
    val chatTitle: String = "",
    val scheduledMeeting: ScheduledMeetingItem? = null,
    val isHost: Boolean = false,
    val isOpenInvite: Boolean = false,
    val inviteParticipantAction: InviteParticipantsAction? = null,
    val participantItemList: List<ChatParticipant> = emptyList(),
    val seeMoreVisible: Boolean = true,
    val enabledMeetingLinkOption: Boolean = true,
    val enabledChatNotificationsOption: Boolean = true,
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val snackBar: Int? = null,
    val result: Long? = null,
    val firstParticipant: ChatParticipant? = ChatParticipant(
        participantId = -1,
        contact = ContactItem(handle = -1,
            email = "first@mega.nz",
            ContactData(fullName = "First", alias = null, avatarUri = null),
            defaultAvatarColor = Constants.AVATAR_PRIMARY_COLOR,
            visibility = UserVisibility.Visible,
            timestamp = -1,
            areCredentialsVerified = false,
            status = UserStatus.Online,
            lastSeen = null),
        privilege = ChatRoomPermission.Moderator
    ),
    val lastParticipant: ChatParticipant? = ChatParticipant(
        participantId = -1,
        contact = ContactItem(handle = -1,
            email = "last@mega.nz",
            ContactData(fullName = "Last", alias = null, avatarUri = null),
            defaultAvatarColor = Constants.AVATAR_PRIMARY_COLOR,
            visibility = UserVisibility.Visible,
            timestamp = -1,
            areCredentialsVerified = false,
            status = UserStatus.Online,
            lastSeen = null),
        privilege = ChatRoomPermission.Moderator
    ),

    ) {

    /**
     * Check if the meeting does not contain participants
     *
     * @return  true if its empty, false otherwise
     */
    fun isEmptyMeeting(): Boolean = firstParticipant == null

    /**
     * Check if meeting contains only 1 user and myself
     *
     * @return  true if its single, false otherwise
     */
    fun isSingleMeeting(): Boolean =
        lastParticipant == null

    /**
     * ScheduledMeetingItem DiffCallback
     */
    class DiffCallback : DiffUtil.ItemCallback<ScheduledMeetingItem>() {
        /**
         * ScheduledMeetingItems are the same
         *
         * @param oldItem [ScheduledMeetingItem]
         * @param newItem [ScheduledMeetingItem]
         * @return True if are the same, false otherwise.
         */
        override fun areItemsTheSame(
            oldItem: ScheduledMeetingItem,
            newItem: ScheduledMeetingItem,
        ): Boolean =
            oldItem.chatId == newItem.chatId

        /**
         * ScheduledMeetingItems contents are the same
         *
         * @param oldItem [ScheduledMeetingItem]
         * @param newItem [ScheduledMeetingItem]
         * @return True if the contents are the same, false otherwise.
         */
        override fun areContentsTheSame(
            oldItem: ScheduledMeetingItem,
            newItem: ScheduledMeetingItem,
        ): Boolean =
            oldItem == newItem
    }
}
