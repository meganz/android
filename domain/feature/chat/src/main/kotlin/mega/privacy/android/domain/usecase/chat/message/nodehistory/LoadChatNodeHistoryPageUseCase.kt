package mega.privacy.android.domain.usecase.chat.message.nodehistory

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryPage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.exception.chat.ChatNodeHistoryLoadException
import mega.privacy.android.domain.repository.chat.ChatNodeHistoryRepository
import javax.inject.Inject

/**
 * Use case to load the next page of the node history of a chat.
 */
class LoadChatNodeHistoryPageUseCase @Inject internal constructor(
    private val chatNodeHistoryRepository: ChatNodeHistoryRepository,
    private val createNodeHistoryAttachmentMessageUseCase: CreateNodeHistoryAttachmentMessageUseCase,
) {

    /**
     * Invoke.
     *
     * Keeps the node history open until the page is loaded and requests batches until
     * [pageSize] messages have been loaded or no more history is available. It also stops when a
     * batch loads no attachment, so that the SDK cannot cause an endless loop. Loading starts only
     * once this collection has received [ChatNodeHistoryEvent.HistoryOpened], so the first
     * batch cannot be emitted before it is being collected. Live attachment events are ignored.
     *
     * Each message gives only the file of its first node, see
     * [CreateNodeHistoryAttachmentMessageUseCase]. A message without a file is left out and does
     * not count towards [pageSize].
     *
     * @param chatId Chat id.
     * @param pageSize Number of messages to load.
     * @return The loaded [ChatNodeHistoryPage].
     * @throws ChatRoomDoesNotExistException if the chat does not exist.
     * @throws ChatNodeHistoryLoadException if the node history cannot be loaded.
     */
    suspend operator fun invoke(chatId: Long, pageSize: Int): ChatNodeHistoryPage {
        val messages = mutableListOf<NodeAttachmentMessage>()
        var status: ChatHistoryLoadStatus? = null
        var batchAttachmentCount = 0

        suspend fun loadRemaining() {
            batchAttachmentCount = 0
            status = chatNodeHistoryRepository.loadNodeHistory(chatId, pageSize - messages.size)
                .also { checkLoadStatus(it) }
        }

        chatNodeHistoryRepository.monitorNodeHistory(chatId).first { event ->
            val isLoading = status != null
            when (event) {
                ChatNodeHistoryEvent.HistoryOpened -> {
                    if (!isLoading) loadRemaining()
                    false
                }

                is ChatNodeHistoryEvent.AttachmentLoaded -> {
                    if (isLoading) {
                        batchAttachmentCount++
                        createNodeHistoryAttachmentMessageUseCase(chatId, event.message)
                            ?.let { messages.add(it) }
                    }
                    false
                }

                ChatNodeHistoryEvent.LoadBatchFinished -> when {
                    !isLoading -> false
                    messages.size < pageSize
                            && status != ChatHistoryLoadStatus.NONE
                            && batchAttachmentCount > 0 -> {
                        loadRemaining()
                        false
                    }

                    else -> true
                }

                is ChatNodeHistoryEvent.AttachmentReceived,
                is ChatNodeHistoryEvent.AttachmentDeleted,
                is ChatNodeHistoryEvent.HistoryTruncated,
                    -> false
            }
        }

        return ChatNodeHistoryPage(
            messages = messages.toList(),
            hasMore = status != ChatHistoryLoadStatus.NONE,
        )
    }

    private fun checkLoadStatus(status: ChatHistoryLoadStatus) {
        when (status) {
            ChatHistoryLoadStatus.INVALID_CHAT -> throw ChatRoomDoesNotExistException()
            ChatHistoryLoadStatus.ERROR -> throw ChatNodeHistoryLoadException()
            ChatHistoryLoadStatus.NONE,
            ChatHistoryLoadStatus.LOCAL,
            ChatHistoryLoadStatus.REMOTE,
                -> Unit
        }
    }
}
