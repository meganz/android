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
        else -> TermCodeType.Unknown
    }
}