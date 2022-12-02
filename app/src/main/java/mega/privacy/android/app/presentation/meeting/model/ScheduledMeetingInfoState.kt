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
 * @property chatId                                     Chat id.
 * @property scheduledMeeting                           Current scheduled meeting item.
 * @property finish                                     True, if the activity is to be terminated.
 * @property openAddContact                             True, if should open Add contact screen. False, if not.
 * @property dndSeconds                                 Do not disturb seconds.
 * @property retentionTimeSeconds                       Retention time seconds.
 * @property meetingLink                                Meeting link.
 * @property chatTitle                                  Chat title.
 * @property isHost                                     If participant has host permissions.
 * @property isOpenInvite                               If open invite option is enabled.
 * @property isPublic                                   If chat room is public.
 * @property seeMoreVisible                             True if see more option is visible, false otherwise.
 * @property enabledMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property snackBar                                   String resource id for showing an snackBar.
 * @property leaveGroupDialog                           True if show leave group alert dialog, false if not.
 * @property addParticipantsNoContactsDialog            True if show add participants no contacts dialog, false if not.
 * @property addParticipantsNoContactsLeftToAddDialog   True if show add participants no contacts left to add dialog, false if not.
 * @property buttons                                    List of available action buttons.
 * @property participantItemList                        List of [ContactItem].
 * @property firstParticipant                           First participant in the chat room.
 * @property lastParticipant                            Last participant in the chat room.
 */
data class ScheduledMeetingInfoState(
    val chatId: Long = -1,
    val scheduledMeeting: ScheduledMeetingItem? = null,
    val finish: Boolean = false,
    val openAddContact: Boolean? = null,
    val dndSeconds: Long? = null,
    val retentionTimeSeconds: Long? = null,
    val meetingLink: String? = null,
    val chatTitle: String = "",
    val isHost: Boolean = false,
    val isOpenInvite: Boolean = false,
    val isPublic: Boolean = false,
    val seeMoreVisible: Boolean = true,
    val enabledMeetingLinkOption: Boolean = true,
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val snackBar: Int? = null,
    val leaveGroupDialog: Boolean = false,
    val addParticipantsNoContactsDialog: Boolean = false,
    val addParticipantsNoContactsLeftToAddDialog: Boolean = false,
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val participantItemList: List<ChatParticipant> = emptyList(),
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
