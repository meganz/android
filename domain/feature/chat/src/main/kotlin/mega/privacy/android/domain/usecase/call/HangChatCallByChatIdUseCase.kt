package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Hang call use case by chat id
 * UseCase used to hang the chat call with another user
 */
class HangChatCallByChatIdUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId id of the chat which should be cut
     * @return [ChatCall]
     */
    suspend operator fun invoke(chatId: Long): ChatCall? =
        if (chatId != -1L) {
            callRepository.getChatCall(chatId)?.apply { callRepository.hangChatCall(callId) }
        } else {
            Log.e("HangChatCallByChatIdUseCase", IllegalStateException("Invalid Chat Id"))
            null
        }
}