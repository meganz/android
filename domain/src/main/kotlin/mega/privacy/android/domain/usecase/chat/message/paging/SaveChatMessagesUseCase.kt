package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to save chat messages
 *
 * @property chatRepository Repository to manage all chat related calls
 * @property createSaveMessageRequestUseCase Use case to map chat messages
 */
class SaveChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createSaveMessageRequestUseCase: CreateSaveMessageRequestUseCase,
) {
    /**
     * Invoke
     *
     * @param response
     */
    suspend operator fun invoke(
        response: FetchMessagePageResponse,
    ) {
        with(response) {
            val currentUserHandle = chatRepository.getMyUserHandle()
            val nextMessage = messages.lastOrNull()?.let {
                chatRepository.getNextMessagePagingInfo(chatId, it.timestamp)
            }
            val requestList = createSaveMessageRequestUseCase(
                chatMessages = messages,
                currentUserHandle = currentUserHandle,
                nextMessageUserHandle = nextMessage?.userHandle
            )

            chatRepository.storeMessages(chatId, requestList)
        }
    }
}