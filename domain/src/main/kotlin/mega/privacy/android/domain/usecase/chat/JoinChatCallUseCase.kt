package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoom
import javax.inject.Inject

/**
 * Use case to join chat calls
 *
 * @property chatRepository     [ChatRepository]
 * @property getChatRoom        [GetChatRoom]
 */
class JoinChatCallUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getChatRoom: GetChatRoom,
) {
    /**
     * Invoke
     *
     * @param chatLink
     */
    suspend operator fun invoke(
        chatLink: String,
    ) {
        val chatRequest = chatRepository.openChatPreview(chatLink).request
        val chatId = chatRequest.chatHandle ?: error("Invalid Chat")
        val chatPublicHandle = chatRequest.userHandle

        val chatRoom = getChatRoom(chatId) ?: throw ChatRoomDoesNotExistException()
        when {
            !chatRoom.isPreview -> {
                // Already joined, do nothing
            }

            !chatRoom.isActive -> {
                chatRepository.autorejoinPublicChat(chatId, chatPublicHandle)
            }

            else -> {
                chatRepository.autojoinPublicChat(chatId)
            }
        }
    }
}
