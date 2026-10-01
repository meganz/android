package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * Use case to get the 1 on 1 chat ID
 */
class Get1On1ChatIdUseCase @Inject constructor(
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
) {

    /**
     * Invoked the chat repository to get the chat ID
     *
     * @param userHandle
     */
    suspend operator fun invoke(userHandle: Long): Long {
        return getChatRoomByUserUseCase(userHandle)?.chatId ?: createChatRoomUseCase(
            isGroup = false,
            userHandles = listOf(userHandle)
        )
    }
}
