package mega.privacy.android.domain.entity.call

/**
 * Num participants changes result
 *
 * @property chatId        Chat ID of the call
 * @property callId        Call ID of the call
 * @property onlyMeInTheCall    True, if I'm the only one in the call. False, if there are more participants.
 * @property isReceivedChange True, if the changes is received. False, if no change has been received.
 */
data class ParticipantsCountChange(
    val chatId: Long,
    val callId: Long,
    val onlyMeInTheCall: Boolean,
    var isReceivedChange: Boolean,
)