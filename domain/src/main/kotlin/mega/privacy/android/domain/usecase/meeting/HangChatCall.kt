package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Use case for hang a call
 */
interface HangChatCall {

    /**
     * Invoke.
     *
     * @param callId  The call id.
     * @return [ChatCall]
     */
    suspend operator fun invoke(
        callId: Long,
    ): ChatCall?
}