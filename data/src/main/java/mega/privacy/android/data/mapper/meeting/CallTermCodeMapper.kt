package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.TermCodeType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert term code to [TermCodeType]
 */
internal class CallTermCodeMapper @Inject constructor() {
    operator fun invoke(termCode: Int): TermCodeType = when (termCode) {
        MegaChatCall.TERM_CODE_INVALID -> TermCodeType.Invalid
        MegaChatCall.TERM_CODE_HANGUP -> TermCodeType.Hangup
        MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS -> TermCodeType.TooManyParticipants
        MegaChatCall.TERM_CODE_REJECT -> TermCodeType.Reject
        MegaChatCall.TERM_CODE_ERROR -> TermCodeType.Error
        MegaChatCall.TERM_CODE_NO_PARTICIPATE -> TermCodeType.NoParticipate
        MegaChatCall.TERM_CODE_TOO_MANY_CLIENTS -> TermCodeType.TooManyClients
        MegaChatCall.TERM_CODE_PROTOCOL_VERSION -> TermCodeType.ProtocolVersion
        MegaChatCall.TERM_CODE_KICKED -> TermCodeType.Kicked
        MegaChatCall.TERM_CODE_WR_TIMEOUT -> TermCodeType.WaitingRoomTimeout
        else -> TermCodeType.Unknown
    }
}