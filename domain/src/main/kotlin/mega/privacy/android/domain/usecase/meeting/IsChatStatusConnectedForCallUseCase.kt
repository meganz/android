package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Is chat status connected for call
 *
 * Use-case make sure chat is connected before starting a call
 * If chat is not connected we wait for the OnChatConnectionStateUpdate to get the updated state as Online
 */
class IsChatStatusConnectedForCallUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke
     *
     * @param chatId chatroom identifier
     */
    suspend operator fun invoke(chatId: Long): Boolean =
        chatRepository.getConnectionState() == ConnectionState.Connected
                && chatRepository.getChatConnectionState(chatId = chatId) == ChatConnectionStatus.Online
}