package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to get chat call
 */
class GetChatCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id
     * @return  [ChatCall]
     */
    suspend operator fun invoke(
        chatId: Long,
    ): ChatCall? = callRepository.getChatCall(chatId)
}