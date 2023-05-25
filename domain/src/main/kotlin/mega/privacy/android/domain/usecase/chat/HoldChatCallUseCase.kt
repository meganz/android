package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Hold call use case
 * UseCase used to hold the chat call with another user
 */
class HoldChatCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId        Chat Id of the call which should be hold
     * @param setOnHold     Flag to set/unset call on hold
     * @return              [ChatCall]
     */
    suspend operator fun invoke(chatId: Long, setOnHold: Boolean): ChatCall? =
        when (chatId) {
            -1L -> error("Invalid Chat Id")
            else -> {
                val chatCallRequest = callRepository.holdChatCall(chatId, setOnHold)
                callRepository.getChatCall(chatCallRequest.chatHandle)
            }
        }
}
