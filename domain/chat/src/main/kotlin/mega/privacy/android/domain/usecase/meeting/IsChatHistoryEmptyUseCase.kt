package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase.Companion.NUMBER_MESSAGES_TO_LOAD
import javax.inject.Inject

/**
 * Monitor if chat history is empty (only management messages)
 *
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
    operator fun invoke(chatId: Long): Flow<Boolean> = flow {
        chatRepository.monitorOnMessageLoaded(chatId)
            .collect { message ->
                message?.let { chatMessage ->
                    pendingToLoad--

                    if (!chatMessage.isManagementMessage) {
                        emit(false)
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
                            emit(true)
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