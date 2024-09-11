package mega.privacy.android.data.facade.chat

import androidx.room.withTransaction
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.chat.ChatDatabase
import mega.privacy.android.data.database.dao.ChatMessageMetaDao
import mega.privacy.android.data.database.dao.ChatNodeDao
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.NodeMessageCrossRef
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndNodeHandleRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import javax.inject.Inject

/**
 * Chat storage facade
 *
 * Facade to encapsulate chat storage implementation
 *
 * @property database In memory chat database
 */
internal class ChatStorageFacade @Inject constructor(
    private val database: Lazy<ChatDatabase>,
) : ChatStorageGateway {

    /**
     * Get typed message request paging source
     *
     * @param chatId Chat ID
     * @return paging source
     */
    override fun getTypedMessageRequestPagingSource(chatId: Long) =
        database.get().typedMessageDao().getAllAsPagingSource(chatId)

    /**
     * Store messages
     *
     * @param messages Messages to store
     */
    override suspend fun storeMessages(
        messages: List<TypedMessageEntity>,
        richPreviews: List<RichPreviewEntity>,
        giphys: List<GiphyEntity>,
        geolocations: List<ChatGeolocationEntity>,
        chatNodes: List<ChatNodeEntity>,
    ) {
        with(database.get()) {
            val chatNodeDao = chatNodeDao()
            val typedMessageDao = typedMessageDao()
            val metaDao = chatMessageMetaDao()
            withTransaction {
                typedMessageDao.deleteStaleMessagesByTempIds(messages.map { it.tempId }
                    .filterNot { it == -1L })
                typedMessageDao.insertAll(messages)
                richPreviews.takeUnless { it.isEmpty() }
                    ?.let { metaDao.insertRichPreviews(it) }
                giphys.takeUnless { it.isEmpty() }?.let { metaDao.insertGiphys(it) }
                geolocations.takeUnless { it.isEmpty() }
                    ?.let { metaDao.insertGeolocations(it) }
                chatNodes.takeUnless { it.isEmpty() }?.let { nodes ->
                    chatNodeDao.insertChatNodes(nodes)
                    nodes.forEach {
                        it.messageId?.let { messageId ->
                            chatNodeDao.insertNodeMessageCrossRef(
                                NodeMessageCrossRef(
                                    messageId = messageId,
                                    id = it.id
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear chat messages
     *
     * @param chatId Chat ID
     */
    override suspend fun clearChatMessages(chatId: Long) {
        with(database.get()) {
            val chatNodeDao = chatNodeDao()
            val metaDao = chatMessageMetaDao()
            val typedMessageDao = typedMessageDao()
            withTransaction {
                val messagesToDelete = typedMessageDao.getMsgIdsByChatId(chatId)
                typedMessageDao.deleteMessagesByChatId(chatId)
                cascadeMessageDeletion(metaDao, messagesToDelete, chatNodeDao)
            }
        }
    }

    /**
     * Get next message
     *
     * @param chatId Chat ID
     * @param timestamp Timestamp
     * @return next message
     */
    override suspend fun getNextMessage(chatId: Long, timestamp: Long) =
        database.get().typedMessageDao().getMessageWithNextGreatestTimestamp(chatId, timestamp)


    override suspend fun storePendingMessage(
        pendingMessageEntity: PendingMessageEntity,
    ) = database.get().pendingMessageDao().insert(pendingMessageEntity)

    override suspend fun storePendingMessages(
        pendingMessageEntities: List<PendingMessageEntity>,
    ) = database.get().pendingMessageDao().insert(pendingMessageEntities)

    override suspend fun updatePendingMessage(vararg updatePendingMessageRequests: UpdatePendingMessageRequest) {
        updatePendingMessageRequests.singleOrNull()?.let { updatePendingMessageRequest ->
            when (updatePendingMessageRequest) {
                is UpdatePendingMessageStateRequest ->
                    database.get().pendingMessageDao().update(updatePendingMessageRequest)

                is UpdatePendingMessageStateAndNodeHandleRequest ->
                    database.get().pendingMessageDao().update(updatePendingMessageRequest)

                is UpdatePendingMessageTransferTagRequest ->
                    database.get().pendingMessageDao().update(updatePendingMessageRequest)

                is UpdatePendingMessageStateAndPathRequest ->
                    database.get().pendingMessageDao().update(updatePendingMessageRequest)
            }
        } ?: run {
            database.get().pendingMessageDao().updateMultiple(updatePendingMessageRequests.toList())
        }
    }

    override suspend fun deletePendingMessage(pendingMessageId: Long) {
        database.get().pendingMessageDao().delete(pendingMessageId)
    }

    override fun fetchPendingMessages(chatId: Long): Flow<List<PendingMessageEntity>> =
        database.get().pendingMessageDao().fetchPendingMessagesForChat(chatId)

    override fun fetchPendingMessages(vararg states: PendingMessageState): Flow<List<PendingMessageEntity>> =
        database.get().pendingMessageDao().fetchPendingMessagesByState(states.toList())

    override suspend fun getPendingMessage(pendingMessageId: Long): PendingMessageEntity? =
        database.get().pendingMessageDao().get(pendingMessageId)

    override suspend fun getPendingMessagesByState(state: PendingMessageState): List<PendingMessageEntity> =
        database.get().pendingMessageDao().getByState(state)

    override suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long> =
        database.get().typedMessageDao().getMessageIdsByType(chatId, type)

    override suspend fun getMessageReactions(chatId: Long, msgId: Long): String? =
        database.get().typedMessageDao().getMessageReactions(chatId, msgId)

    override suspend fun updateMessageReactions(
        chatId: Long,
        msgId: Long,
        reactions: String,
    ) {
        database.get().typedMessageDao().updateMessageReactions(chatId, msgId, reactions)
    }

    override suspend fun truncateMessages(chatId: Long, truncateTimestamp: Long) {
        with(database.get()) {
            val chatNodeDao = chatNodeDao()
            val metaDao = chatMessageMetaDao()
            val typedMessageDao = typedMessageDao()
            withTransaction {
                val messagesToDelete =
                    typedMessageDao.getMsgIdsByChatIdAndLatestDate(chatId, truncateTimestamp)
                typedMessageDao.deleteMessagesById(messagesToDelete)
                cascadeMessageDeletion(metaDao, messagesToDelete, chatNodeDao)
            }
        }
    }

    private fun cascadeMessageDeletion(
        metaDao: ChatMessageMetaDao,
        messagesToDelete: List<Long>,
        chatNodeDao: ChatNodeDao,
    ) {
        metaDao.deleteRichPreviewsByMessageId(messagesToDelete)
        metaDao.deleteGiphysByMessageId(messagesToDelete)
        metaDao.deleteGeolocationsByMessageId(messagesToDelete)
        chatNodeDao.removeMessageNodeRelationship(messagesToDelete)
        chatNodeDao.deleteOrphanedNodes()
    }

    override suspend fun clearChatPendingMessages(chatId: Long) {
        database.get().pendingMessageDao().deleteAllForChat(chatId)
    }

    override suspend fun updateExistsInMessage(chatId: Long, msgId: Long, exists: Boolean) {
        database.get().typedMessageDao().updateExists(chatId, msgId, exists)
    }

    override suspend fun getExistsInMessage(chatId: Long, msgId: Long) =
        database.get().typedMessageDao().getExists(chatId, msgId)

    override suspend fun clearAllData() {
        database.get().clearAllTables()
    }
}