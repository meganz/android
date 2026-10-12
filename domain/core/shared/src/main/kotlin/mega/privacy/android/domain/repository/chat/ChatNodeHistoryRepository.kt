package mega.privacy.android.domain.repository.chat

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent

/**
 * Repository for the node history of a chat: the messages that carry attached nodes.
 */
interface ChatNodeHistoryRepository {

    /**
     * Monitor the node history of a chat.
     *
     * The node history is open while the flow is collected and closed once collection stops.
     * Collectors of the same chat share one open node history. Every message is delivered with
     * its full node list.
     *
     * @param chatId Chat id.
     * @return Flow of [ChatNodeHistoryEvent].
     */
    fun monitorNodeHistory(chatId: Long): Flow<ChatNodeHistoryEvent>

    /**
     * Request the next batch of the node history of a chat.
     *
     * The loaded messages are emitted by [monitorNodeHistory] as
     * [ChatNodeHistoryEvent.AttachmentLoaded], followed by
     * [ChatNodeHistoryEvent.LoadBatchFinished], so call this only after [monitorNodeHistory] has
     * emitted [ChatNodeHistoryEvent.HistoryOpened].
     *
     * @param chatId Chat id.
     * @param count Number of messages to load.
     * @return The source the messages are going to be fetched from.
     */
    suspend fun loadNodeHistory(chatId: Long, count: Int): ChatHistoryLoadStatus
}
