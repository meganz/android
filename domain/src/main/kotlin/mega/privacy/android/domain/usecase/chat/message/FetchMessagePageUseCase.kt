package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Fetch message page use case
 *
 * @property chatRepository [ChatRepository]
 * @property getMessageListUseCase [GetMessageListUseCase]
 * @property mapChatMessageListUseCase [MapChatMessageListUseCase]
 */
class FetchMessagePageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getMessageListUseCase: GetMessageListUseCase,
    private val mapChatMessageListUseCase: MapChatMessageListUseCase,
) {

    /**
     * Invoke
     *
     * @param messageFlow
     * @return mapped list of next 32 messages
     */
    suspend operator fun invoke(
        messageFlow: Flow<ChatMessage?>,
        nextMessageUserHandle: Long?,
    ): List<TypedMessage> {
        val currentUserHandle = chatRepository.getMyUserHandle()
        val chatMessages = getMessageListUseCase(messageFlow)
        return mapChatMessageListUseCase(chatMessages, currentUserHandle, nextMessageUserHandle)
    }
}