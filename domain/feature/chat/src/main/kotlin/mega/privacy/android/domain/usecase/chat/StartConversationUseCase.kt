package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * Use case for starting a new chat conversation.
 */
class StartConversationUseCase @Inject constructor(
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
) {

    /**
     * Invoke.
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of user handles.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(isGroup: Boolean, userHandles: List<Long>): Long {
        val chat1to1 = getChatRoomByUserUseCase(userHandle = userHandles[0])
        return if (!isGroup && chat1to1 != null) {
            chat1to1.chatId
        } else {
            createChatRoomUseCase(isGroup = isGroup, userHandles = userHandles)
        }
    }

}
