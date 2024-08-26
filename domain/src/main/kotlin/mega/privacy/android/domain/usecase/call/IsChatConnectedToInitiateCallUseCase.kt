package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * IsChatConnectedInOrderToInitiateACall
 */
class IsChatConnectedToInitiateCallUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Method to check if the chat is online
     *
     * @param newState The state of chat
     * @param chatRoom The MegaChatRoom
     * @param userWaitingForCall id of the user waiting for call
     * @return True, if the chat is connected and a call can be started. False, otherwise
     */
    suspend operator fun invoke(
        newState: ChatConnectionStatus,
        chatRoom: ChatRoom?,
        userWaitingForCall: Long,
    ): Boolean {
        val chatId = chatRoom?.chatId ?: return false
        val peerHandle = chatRepository.getPeerHandle(chatId, 0) ?: return false
        return newState == ChatConnectionStatus.Online && peerHandle != INVALID_CHAT_HANDLE && peerHandle == userWaitingForCall
    }

    companion object {
        /**
         * Invalid chat handle
         */
        private const val INVALID_CHAT_HANDLE = -1L
    }
}