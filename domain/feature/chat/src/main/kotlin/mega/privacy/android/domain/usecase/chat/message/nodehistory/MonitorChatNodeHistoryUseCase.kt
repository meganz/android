package mega.privacy.android.domain.usecase.chat.message.nodehistory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryFileEvent
import mega.privacy.android.domain.repository.chat.ChatNodeHistoryRepository
import javax.inject.Inject

/**
 * Use case to monitor the node history of a chat: the messages that carry attached nodes.
 */
class MonitorChatNodeHistoryUseCase @Inject internal constructor(
    private val chatNodeHistoryRepository: ChatNodeHistoryRepository,
    private val createNodeHistoryAttachmentMessageUseCase: CreateNodeHistoryAttachmentMessageUseCase,
) {

    /**
     * Invoke.
     *
     * The node history stays open while the returned flow is collected and is closed once
     * collection stops. Each received message gives only the file of its first node, see
     * [CreateNodeHistoryAttachmentMessageUseCase]. A received message without a file is not
     * emitted. Use [LoadChatNodeHistoryPageUseCase] to load the older history.
     *
     * @param chatId Chat id.
     * @return Flow of [ChatNodeHistoryFileEvent].
     */
    operator fun invoke(chatId: Long): Flow<ChatNodeHistoryFileEvent> =
        chatNodeHistoryRepository.monitorNodeHistory(chatId).mapNotNull { event ->
            when (event) {
                ChatNodeHistoryEvent.HistoryOpened -> ChatNodeHistoryFileEvent.HistoryOpened
                is ChatNodeHistoryEvent.AttachmentReceived ->
                    createNodeHistoryAttachmentMessageUseCase(chatId, event.message)
                        ?.let { ChatNodeHistoryFileEvent.FileReceived(it) }

                is ChatNodeHistoryEvent.AttachmentDeleted ->
                    ChatNodeHistoryFileEvent.FileDeleted(event.messageId)

                is ChatNodeHistoryEvent.HistoryTruncated ->
                    ChatNodeHistoryFileEvent.HistoryTruncated(event.messageId)

                is ChatNodeHistoryEvent.AttachmentLoaded,
                ChatNodeHistoryEvent.LoadBatchFinished,
                    -> null
            }
        }
}
