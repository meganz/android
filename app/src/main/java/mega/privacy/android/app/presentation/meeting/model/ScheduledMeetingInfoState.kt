package mega.privacy.android.app.presentation.meeting.model

import androidx.recyclerview.widget.DiffUtil
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Data class defining the state of [ScheduledMeetingInfoViewModel]
 *
 * @property chatId                                     Chat id.
 * @property scheduledMeeting                           [ChatScheduledMeeting].
 * @property finish                                     True, if the activity is to be terminated.
 * @property openAddContact                             True, if should open Add contact screen. False, if not.
 * @property dndSeconds                                 Do not disturb seconds.
 * @property retentionTimeSeconds                       Retention time seconds.
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
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property leaveGroupDialog                           True if show leave group alert dialog, false if not.
 * @property addParticipantsNoContactsDialog            True if show add participants no contacts dialog, false if not.
 * @property addParticipantsNoContactsLeftToAddDialog   True if show add participants no contacts left to add dialog, false if not.
 * @property buttons                                    List of available action buttons.
 * @property participantItemList                        List of [ContactItem].
 * @property firstParticipant                           First participant in the chat room.
 * @property secondParticipant                          Second participant in the chat room.
 * @property numOfParticipants                          Number of participants.
 * @property is24HourFormat                             True, if it's 24 hour format.
 * @property enabledWaitingRoomOption                   True if is enabled waiting room option, false otherwise.
 * @property snackbarMsg                                State to show snackbar message
 */
data class ScheduledMeetingInfoState(
    val chatId: Long = -1L,
    val scheduledMeeting: ChatScheduledMeeting? = null,
    val finish: Boolean = false,
    val openAddContact: Boolean? = null,
    val dndSeconds: Long? = null,
    val retentionTimeSeconds: Long? = null,
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
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val leaveGroupDialog: Boolean = false,
    val addParticipantsNoContactsDialog: Boolean = false,
    val addParticipantsNoContactsLeftToAddDialog: Boolean = false,
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val participantItemList: List<ChatParticipant> = emptyList(),
    val firstParticipant: ChatParticipant? = null,
    val secondParticipant: ChatParticipant? = null,
    val numOfParticipants: Int = 0,
    val is24HourFormat: Boolean = false,
    val enabledWaitingRoomOption: Boolean = true,
    val snackbarMsg: StateEventWithContent<String> = consumed(),
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
        secondParticipant == null

    /**
     * ChatScheduledMeeting DiffCallback
     */
    class DiffCallback : DiffUtil.ItemCallback<ChatScheduledMeeting>() {
        /**
         * ChatScheduledMeetings are the same
         *
         * @param oldItem [ChatScheduledMeeting]
         * @param newItem [ChatScheduledMeeting]
         * @return True if are the same, false otherwise.
         */
        override fun areItemsTheSame(
            oldItem: ChatScheduledMeeting,
            newItem: ChatScheduledMeeting,
        ): Boolean =
            oldItem.chatId == newItem.chatId

        /**
         * ChatScheduledMeetings contents are the same
         *
         * @param oldItem [ChatScheduledMeeting]
         * @param newItem [ChatScheduledMeeting]
         * @return True if the contents are the same, false otherwise.
         */
        override fun areContentsTheSame(
            oldItem: ChatScheduledMeeting,
            newItem: ChatScheduledMeeting,
        ): Boolean =
            oldItem == newItem
    }
}
