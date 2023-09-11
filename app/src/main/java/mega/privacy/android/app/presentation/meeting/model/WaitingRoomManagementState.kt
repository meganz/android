package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingViewModel

/**
 * Data class defining the state of [CreateScheduledMeetingViewModel]
 *
 * @property showParticipantsInWaitingRoomDialog     Show dialog when there are participants in the waiting room
 * @property showDenyParticipantDialog               Show dialog to deny entry to a participant in the call.
 * @property snackbarString                          Snackbar string
 * @property scheduledMeetingTitle                   Scheduled meeting title
 * @property chatId                                  Chat id of call with waiting room
 * @property usersInWaitingRoom                      User list in the waiting room
 * @property nameOfTheOnlyUserInTheWaitingRoom       User name
 * @property chatIdOfCallOpened                      Chat id of call opened
 */
data class WaitingRoomManagementState constructor(
    val snackbarString: String? = null,
    val showParticipantsInWaitingRoomDialog: Boolean = false,
    val showDenyParticipantDialog: Boolean = false,
    val usersInWaitingRoom: List<Long> = emptyList(),
    val chatId: Long = -1L,
    val scheduledMeetingTitle: String = "",
    val nameOfTheOnlyUserInTheWaitingRoom: String = "",
    val chatIdOfCallOpened: Long = -1L,
) {
    /**
     * Check if the dialog is relative to the open call
     *
     * @return True if the dialog is relative to the open call
     */
    fun isDialogRelativeToTheOpenCall() = chatId != -1L && chatId == chatIdOfCallOpened
}