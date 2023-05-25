package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Hang call use case
 * UseCase used to hang the chat call with another user
 */
class HangChatCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param callId id of the call which should be cut
     * @return [ChatCall]
     */
    suspend operator fun invoke(callId: Long): ChatCall? =
        when (callId) {
            -1L -> error("Invalid Call Id")
            else -> {
                val chatCallRequest = callRepository.hangChatCall(callId)
                callRepository.getChatCall(chatCallRequest.chatHandle)
            }
        }
}
