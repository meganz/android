package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to get video updates from local device for an specific chat room.
 *
 * @property callRepository     [CallRepository]
 */
class GetChatLocalVideoUpdatesUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Get Video Updates from local device
     *
     * @note To receive video before starting a call, use MEGACHAT_INVALID_HANDLE
     *
     * @param chatId    Chat Room Id
     * @return          Flow of [ChatVideoUpdate]
     */
    operator fun invoke(chatId: Long = -1L): Flow<ChatVideoUpdate> =
        callRepository.getChatLocalVideoUpdates(chatId)
}
