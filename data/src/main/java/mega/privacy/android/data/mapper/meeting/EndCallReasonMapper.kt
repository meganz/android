package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.EndCallReason
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert end call reason to [EndCallReason]
 */
internal class EndCallReasonMapper @Inject constructor() {

    operator fun invoke(endCallReason: Int): EndCallReason = when (endCallReason) {
        MegaChatCall.END_CALL_REASON_INVALID -> EndCallReason.Invalid
        MegaChatCall.END_CALL_REASON_ENDED -> EndCallReason.Ended
        MegaChatCall.END_CALL_REASON_REJECTED -> EndCallReason.Rejected
        MegaChatCall.END_CALL_REASON_NO_ANSWER -> EndCallReason.NoAnswer
        MegaChatCall.END_CALL_REASON_FAILED -> EndCallReason.Failed
        MegaChatCall.END_CALL_REASON_CANCELLED -> EndCallReason.Cancelled
        MegaChatCall.END_CALL_REASON_BY_MODERATOR -> EndCallReason.ByModerator
        else -> EndCallReason.Unknown
    }
}