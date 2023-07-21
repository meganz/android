package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase.Companion.NUMBER_MESSAGES_TO_LOAD
import javax.inject.Inject

/**
 * Monitor if chat history is empty (only management messages)
 *
 * @property chatRepository [ChatRepository]
 * @property loadMessagesUseCase [LoadMessagesUseCase]
 */
class IsChatHistoryEmptyUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val loadMessagesUseCase: LoadMessagesUseCase,
) {

    private var pendingToLoad = NUMBER_MESSAGES_TO_LOAD
    private var chatHistoryLoadStatus: ChatHistoryLoadStatus? = null

    /**
     * Checks if chat history is empty (only management messages)
     *
     * @param chatId    Chat ID
     * @return          True if the chat history is empty (only management messages) or false otherwise.
     */
    operator fun invoke(chatId: Long) = callbackFlow {
        chatRepository.monitorOnMessageLoaded(chatId)
            .collect { message ->
                message?.let { chatMessage ->
                    pendingToLoad--

                    if (!chatMessage.isManagementMessage) {
                        trySend(false)
                        close()
                    } else {
                        //Management message, no action required.
                    }

                    if (pendingToLoad == 0) {
                        loadMessages(chatId)
                    }
                } ?: run {
                    when (chatHistoryLoadStatus) {
                        ChatHistoryLoadStatus.NONE -> {
                            // Full history loaded
                            trySend(true)
                            close()
                        }

                        else -> {
                            loadMessages(chatId)
                        }
                    }
                }
            }
    }

    private suspend fun loadMessages(chatId: Long) {
        pendingToLoad = NUMBER_MESSAGES_TO_LOAD
        chatHistoryLoadStatus = loadMessagesUseCase(chatId)
    }
}