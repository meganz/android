package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingViewModel
import mega.privacy.android.domain.entity.chat.ChatParticipant

/**
 * Data class defining the state of [CreateScheduledMeetingViewModel]
 *
 * @property showParticipantsInWaitingRoomDialog     Show dialog when there are participants in the waiting room
 * @property showDenyParticipantDialog               Show dialog to deny entry to a participant in the call.
 * @property snackbarString                          Snackbar string
 * @property scheduledMeetingTitle                   Scheduled meeting title
 * @property chatId                                  Chat id of call with waiting room
 * @property usersInWaitingRoom                      User list in the waiting room
 * @property temporaryUsersInWaitingRoomList         Temporary list of users in waiting room
 * @property nameOfTheFirstUserInTheWaitingRoom      Name of the first user in the waiting room
 * @property nameOfTheSecondUserInTheWaitingRoom     Name of the second user in the waiting room
 * @property chatIdOfCallOpened                      Chat id of call opened
 * @property usersAdmitted                           True if users were admitted, false if not
 * @property participantToDenyEntry                  [ChatParticipant] to deny entry
 * @property shouldWaitingRoomBeShown                True, it must be shown. False, must be hidden
 * @property isDialogClosed                          True, if waiting room dialog is closed. False if not
 */
data class WaitingRoomManagementState constructor(
    val snackbarString: String? = null,
    val showParticipantsInWaitingRoomDialog: Boolean = false,
    val showDenyParticipantDialog: Boolean = false,
    val usersInWaitingRoom: List<Long> = emptyList(),
    val temporaryUsersInWaitingRoomList: List<Long> = emptyList(),
    val chatId: Long = -1L,
    val scheduledMeetingTitle: String = "",
    val nameOfTheFirstUserInTheWaitingRoom: String = "",
    val nameOfTheSecondUserInTheWaitingRoom: String = "",
    val chatIdOfCallOpened: Long = -1L,
    val usersAdmitted: Boolean = false,
    val participantToDenyEntry: ChatParticipant? = null,
    val shouldWaitingRoomBeShown: Boolean = false,
    val isDialogClosed: Boolean = false,
) {
    /**
     * Check if the dialog is relative to the open call
     *
     * @return True if the dialog is relative to the open call
     */
    fun isDialogRelativeToTheOpenCall() = chatId != -1L && chatId == chatIdOfCallOpened
}