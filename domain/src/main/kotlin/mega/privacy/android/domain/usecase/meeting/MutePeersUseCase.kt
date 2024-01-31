package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Mute peers use case by chat id
 */
class MutePeersUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @param clientId  Client id
     * @return          [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        clientId: Long,
    ): ChatRequest = callRepository.mutePeers(
        chatId,
        clientId,
    )
}