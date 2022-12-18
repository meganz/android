package mega.privacy.android.app.presentation.meeting.model

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem

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
 * @property openSendToChat                             True, open sent to chat screen. False, close it.
 * @property openRemoveParticipantDialog                True, open remove participant dialog. False, close it.
 * @property selected                                   [ChatParticipant] selected.
 * @property openChatRoom                               Chat id of the chat room to send message.
 * @property showChangePermissionsDialog                Show change permissions dialog.
 * @property openChatCall                               Chat id of the chat room to send message.
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
 * @property isEditEnabled                              True if edit scheduled meeting is allowed, false otherwise.
 * @property buttons                                    List of available action buttons.
 * @property participantItemList                        List of [ContactItem].
 * @property firstParticipant                           First participant in the chat room.
 * @property lastParticipant                            Last participant in the chat room.
 * @property numOfParticipants                          Number of participants.
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
    val openSendToChat: Boolean = false,
    val openRemoveParticipantDialog: Boolean = false,
    val selected: ChatParticipant? = null,
    val openChatRoom: Long? = null,
    val showChangePermissionsDialog: ChatRoomPermission? = null,
    val openChatCall: Long? = null,
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
    val isEditEnabled: Boolean = false,
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val participantItemList: List<ChatParticipant> = emptyList(),
    val firstParticipant: ChatParticipant? = null,
    val lastParticipant: ChatParticipant? = null,
    val numOfParticipants: Int = 0,
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
