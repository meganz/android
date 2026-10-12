package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.chat.ChatHistoryLoadStatusMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.model.ChatNodeHistoryUpdate
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.ChatNodeHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatNodeHistoryRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val chatMessageMapper: ChatMessageMapper,
    private val chatHistoryLoadStatusMapper: ChatHistoryLoadStatusMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatNodeHistoryRepository {

    override fun monitorNodeHistory(chatId: Long): Flow<ChatNodeHistoryEvent> =
        megaChatApiGateway.monitorNodeHistory(chatId)
            .map { update ->
                when (update) {
                    ChatNodeHistoryUpdate.Opened -> ChatNodeHistoryEvent.HistoryOpened

                    is ChatNodeHistoryUpdate.Loaded -> update.msg
                        ?.let { ChatNodeHistoryEvent.AttachmentLoaded(chatMessageMapper(it)) }
                        ?: ChatNodeHistoryEvent.LoadBatchFinished

                    is ChatNodeHistoryUpdate.Received ->
                        ChatNodeHistoryEvent.AttachmentReceived(chatMessageMapper(update.msg))

                    is ChatNodeHistoryUpdate.Deleted ->
                        ChatNodeHistoryEvent.AttachmentDeleted(update.msgId)

                    is ChatNodeHistoryUpdate.Truncated ->
                        ChatNodeHistoryEvent.HistoryTruncated(update.msgId)
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun loadNodeHistory(chatId: Long, count: Int): ChatHistoryLoadStatus =
        withContext(ioDispatcher) {
            chatHistoryLoadStatusMapper(megaChatApiGateway.loadAttachments(chatId, count))
        }
}
