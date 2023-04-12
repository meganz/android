package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatSessionTermCode
import nz.mega.sdk.MegaChatSession
import javax.inject.Inject

/**
 * Chat session term code mapper
 */
internal class ChatSessionTermCodeMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param code input term code
     * @return [ChatSessionTermCode]
     */
    operator fun invoke(code: Int) = when (code) {
        MegaChatSession.SESS_TERM_CODE_RECOVERABLE -> ChatSessionTermCode.Recoverable
        MegaChatSession.SESS_TERM_CODE_NON_RECOVERABLE -> ChatSessionTermCode.NonRecoverable
        else -> ChatSessionTermCode.Invalid
    }
}