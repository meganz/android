package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import javax.inject.Inject

/**
 * Join public chat
 *
 * @property chatRepository
 * @property getChatRoomUseCase
 */
class JoinPublicChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getChatRoomUseCase: GetChatRoomUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId
     * @param chatPublicHandle
     * @param autoJoin
     */
    suspend operator fun invoke(
        chatId: Long,
        chatPublicHandle: Long,
        autoJoin: Boolean,
    ) {
        val chatRoom = getChatRoomUseCase(chatId) ?: throw ChatRoomDoesNotExistException()
        when {
            !chatRoom.isPreview -> {
                // Already joined, do nothing
            }

            !chatRoom.isActive -> {
                chatRepository.autorejoinPublicChat(chatId, chatPublicHandle)
            }

            autoJoin -> {
                chatRepository.autojoinPublicChat(chatId)
            }

        }
        chatRepository.setLastPublicHandle(chatId)
    }
}