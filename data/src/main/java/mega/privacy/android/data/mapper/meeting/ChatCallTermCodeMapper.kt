package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert term code to [ChatCallTermCodeType]
 */
internal class ChatCallTermCodeMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param termCode input term code
     * @return [ChatCallTermCodeType]
     */
    operator fun invoke(termCode: Int): ChatCallTermCodeType = when (termCode) {
        MegaChatCall.TERM_CODE_INVALID -> ChatCallTermCodeType.Invalid
        MegaChatCall.TERM_CODE_HANGUP -> ChatCallTermCodeType.Hangup
        MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS -> ChatCallTermCodeType.TooManyParticipants
        MegaChatCall.TERM_CODE_REJECT -> ChatCallTermCodeType.Reject
        MegaChatCall.TERM_CODE_ERROR -> ChatCallTermCodeType.Error
        MegaChatCall.TERM_CODE_NO_PARTICIPATE -> ChatCallTermCodeType.NoParticipate
        MegaChatCall.TERM_CODE_TOO_MANY_CLIENTS -> ChatCallTermCodeType.TooManyClients
        MegaChatCall.TERM_CODE_PROTOCOL_VERSION -> ChatCallTermCodeType.ProtocolVersion
        MegaChatCall.TERM_CODE_KICKED -> ChatCallTermCodeType.Kicked
        MegaChatCall.TERM_CODE_WR_TIMEOUT -> ChatCallTermCodeType.WaitingRoomTimeout
        MegaChatCall.TERM_CODE_CALL_DUR_LIMIT -> ChatCallTermCodeType.CallDurationLimit
        MegaChatCall.TERM_CODE_CALL_USERS_LIMIT -> ChatCallTermCodeType.CallUsersLimit
        else -> ChatCallTermCodeType.Unknown
    }
}