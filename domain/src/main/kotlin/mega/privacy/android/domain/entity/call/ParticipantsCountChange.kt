package mega.privacy.android.domain.entity.call

/**
 * Num participants changes result
 *
 * @property chatId        Chat ID of the call
 * @property onlyMeInTheCall    True, if I'm the only one in the call. False, if there are more participants.
 * @property waitingForOthers True, if I'm waiting for others participants. False, otherwise.
 * @property isReceivedChange True, if the changes is received. False, if no change has been received.
 */
data class ParticipantsCountChange(
    val chatId: Long,
    val onlyMeInTheCall: Boolean,
    val waitingForOthers: Boolean,
    var isReceivedChange: Boolean,
)