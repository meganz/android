package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Mute all peers use case by chat id
 */
class MuteAllPeersUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @return          [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
    ): ChatRequest = callRepository.muteAllPeers(
        chatId,
    )
}