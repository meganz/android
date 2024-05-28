package mega.privacy.android.data.gateway.chat

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest

/**
 * Chat storage gateway
 */
interface ChatStorageGateway {
    /**
     * Get typed message request paging source
     *
     * @param chatId
     * @return
     */
    fun getTypedMessageRequestPagingSource(chatId: Long): PagingSource<Int, MetaTypedMessageEntity>

    /**
     * Store messages
     *
     * @param messages
     * @param richPreviews
     * @param giphys
     * @param geolocations
     * @param chatNodes
     */
    suspend fun storeMessages(
        messages: List<TypedMessageEntity>,
        richPreviews: List<RichPreviewEntity>,
        giphys: List<GiphyEntity>,
        geolocations: List<ChatGeolocationEntity>,
        chatNodes: List<ChatNodeEntity>,
    )

    /**
     * Clear chat messages
     *
     * @param chatId
     */
    suspend fun clearChatMessages(chatId: Long)

    /**
     * Get next message
     *
     * @param chatId
     * @param timestamp
     * @return
     */
    suspend fun getNextMessage(chatId: Long, timestamp: Long): TypedMessageEntity?

    /**
     * Store pending message
     *
     * @param pendingMessageEntity
     *
     * @return the id of the inserted [PendingMessageEntity]
     */
    suspend fun storePendingMessage(
        pendingMessageEntity: PendingMessageEntity,
    ): Long

    /**
     * Store multiple pending messages
     *
     * @param pendingMessageEntities
     *
     * @return the ids of the inserted [PendingMessageEntity]
     */
    suspend fun storePendingMessages(
        pendingMessageEntities: List<PendingMessageEntity>,
    ): List<Long>

    /**
     * Update pending message
     *
     * @param updatePendingMessageRequest
     */
    suspend fun updatePendingMessage(vararg updatePendingMessageRequest: UpdatePendingMessageRequest)

    /**
     * Delete pending message by id
     *
     * @param pendingMessageId
     */
    suspend fun deletePendingMessage(pendingMessageId: Long)

    /**
     * Fetch pending messages for a chat
     *
     * @param chatId
     * @return flow of pending messages for the chat
     */
    fun fetchPendingMessages(chatId: Long): Flow<List<PendingMessageEntity>>

    /**
     * Fetch pending messages of a specific state
     *
     * @param states
     * @return flow of pending messages of the specific state
     */
    fun fetchPendingMessages(vararg states: PendingMessageState): Flow<List<PendingMessageEntity>>

    /**
     * Fetch pending messages by id
     *
     * @param pendingMessageId
     * @return a pending messages with [pendingMessageId] or null if not found
     */
    suspend fun getPendingMessage(pendingMessageId: Long): PendingMessageEntity?

    /**
     * Get all pending messages in a specific state
     */
    suspend fun getPendingMessagesByState(state: PendingMessageState): List<PendingMessageEntity>

    /**
     * Get message ids by type
     *
     * @param chatId
     * @param type
     * @return list of message ids
     */
    suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long>

    /**
     * Get message reactions
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @return String with reactions if any.
     */
    suspend fun getMessageReactions(chatId: Long, msgId: Long): String?

    /**
     * Update message reactions.
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param reactions Reactions
     */
    suspend fun updateMessageReactions(chatId: Long, msgId: Long, reactions: String)

    /**
     * Truncate messages
     *
     * @param chatId
     * @param truncateTimestamp
     */
    suspend fun truncateMessages(chatId: Long, truncateTimestamp: Long)

    /**
     * Clear chat pending messages
     *
     * @param chatId
     */
    suspend fun clearChatPendingMessages(chatId: Long)

    /**
     * Update exists
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param exists True if the content in message exists
     */
    suspend fun updateExistsInMessage(chatId: Long, msgId: Long, exists: Boolean)

    /**
     * Get exists
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @return True if the content in message exists
     */
    suspend fun getExistsInMessage(chatId: Long, msgId: Long): Boolean?

    /**
     * Clear all typed messages
     */
    suspend fun clearAllData()
}