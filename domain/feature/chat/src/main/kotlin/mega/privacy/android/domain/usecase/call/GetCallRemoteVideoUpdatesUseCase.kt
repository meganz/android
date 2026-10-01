package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to get video updates from remote device for an specific chat room.
 *
 * @property callRepository     [CallRepository]
 */
class GetCallRemoteVideoUpdatesUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Get Video Updates from remove client
     * @param chatId    Chat Room Id
     * @param clientId  Client Id
     * @param isHighRes    True if high resolution video is requested, false otherwise
     *
     * @return          Flow of [ChatVideoUpdate]
     */
    operator fun invoke(chatId: Long, clientId: Long, isHighRes: Boolean): Flow<ChatVideoUpdate> =
        callRepository.getChatRemoteVideoUpdates(chatId, clientId, isHighRes)
}
