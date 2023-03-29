package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mega chat call status mapper
 * Maps [MegaChatCall] to [ChatCallStatus]
 * If any unexpected status comes [ChatCallStatus.Unknown] is returned
 * Reverse mapping is done at [MegaChatCallStatusMapper]
 */
internal class ChatCallStatusMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param status [Int] value denoting MegaChatCall status
     * @return [ChatCallStatus]
     */
    operator fun invoke(status: Int): ChatCallStatus = when (status) {
        MegaChatCall.CALL_STATUS_INITIAL -> ChatCallStatus.Initial
        MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> ChatCallStatus.UserNoPresent
        MegaChatCall.CALL_STATUS_CONNECTING -> ChatCallStatus.Connecting
        MegaChatCall.CALL_STATUS_JOINING -> ChatCallStatus.Joining
        MegaChatCall.CALL_STATUS_IN_PROGRESS -> ChatCallStatus.InProgress
        MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION -> ChatCallStatus.TerminatingUserParticipation
        MegaChatCall.CALL_STATUS_DESTROYED -> ChatCallStatus.Destroyed
        else -> ChatCallStatus.Unknown
    }
}