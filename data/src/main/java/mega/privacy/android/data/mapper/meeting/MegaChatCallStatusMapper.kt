package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Chat call int status mapper
 * Maps [ChatCallStatus] to [MegaChatCall]
 * If [ChatCallStatus.Unknown] comes -1 is returned this is an unexpected scenario
 * Reverse mapping is done at [ChatCallStatus]
 */
internal class MegaChatCallStatusMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param status [ChatCallStatus]
     * @return [Int] value which is an instance of [MegaChatCall]
     */
    operator fun invoke(status: ChatCallStatus): Int = when (status) {
        ChatCallStatus.Initial -> MegaChatCall.CALL_STATUS_INITIAL
        ChatCallStatus.UserNoPresent -> MegaChatCall.CALL_STATUS_USER_NO_PRESENT
        ChatCallStatus.Connecting -> MegaChatCall.CALL_STATUS_CONNECTING
        ChatCallStatus.Joining -> MegaChatCall.CALL_STATUS_JOINING
        ChatCallStatus.InProgress -> MegaChatCall.CALL_STATUS_IN_PROGRESS
        ChatCallStatus.TerminatingUserParticipation -> MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
        ChatCallStatus.Destroyed -> MegaChatCall.CALL_STATUS_DESTROYED
        else -> -1
    }
}