package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoom
import javax.inject.Inject

/**
 * Use case to join chat calls for guests
 *
 * @property chatRepository
 * @property getChatRoom
 * @property createEphemeralAccountUseCase
 */
class JoinGuestChatCallUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getChatRoom: GetChatRoom,
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
) {

    /**
     * Invoke
     *
     * @param chatLink
     * @param firstName
     * @param lastName
     */
    suspend operator fun invoke(
        chatLink: String,
        firstName: String,
        lastName: String,
    ) {
        initGuestChatSessionUseCase(anonymousMode = false)

        createEphemeralAccountUseCase(firstName, lastName)

        val chatRequest = chatRepository.openChatPreview(chatLink)
        val chatId = chatRequest.chatHandle ?: error("Invalid Chat")
        val chatPublicHandle = chatRequest.userHandle

        val chatRoom = getChatRoom(chatId) ?: throw ChatRoomDoesNotExistException()
        if (!chatRoom.isPreview && !chatRoom.isActive && chatPublicHandle != null) {
            chatRepository.autorejoinPublicChat(chatId, chatPublicHandle)
        } else {
            chatRepository.autojoinPublicChat(chatId)
        }
    }
}
