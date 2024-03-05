package mega.privacy.android.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.domain.entity.chat.ChatMessageType

/**
 * Typed message request dao
 */
@Dao
interface TypedMessageDao {

    /**
     * Get all as paging source
     *
     * @param chatId
     * @return paging source
     */
    @Transaction
    @Query("SELECT * FROM typed_messages WHERE chatId = :chatId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllAsPagingSource(chatId: Long): PagingSource<Int, MetaTypedMessageEntity>

    /**
     * Insert all
     *
     * @param messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<TypedMessageEntity>)

    /**
     * Delete messages by temp id
     *
     * @param tempIds
     */
    @Query("DELETE FROM typed_messages WHERE tempId IN (:tempIds) AND messageId = tempId")
    suspend fun deleteStaleMessagesByTempIds(tempIds: List<Long>)

    /**
     * Delete messages by chat id
     *
     * @param chatId
     */
    @Query("DELETE FROM typed_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: Long)

    /**
     * Delete messages by chat id
     *
     * @param chatId
     */
    @Query("SELECT messageId FROM typed_messages WHERE chatId = :chatId")
    suspend fun getMsgIdsByChatId(chatId: Long): List<Long>

    /**
     * Get message with next greatest timestamp
     *
     * @param chatId
     * @param timestamp
     * @return message
     */
    @Query("SELECT * FROM typed_messages WHERE chatId = :chatId AND timestamp > :timestamp ORDER BY timestamp ASC LIMIT 1")
    suspend fun getMessageWithNextGreatestTimestamp(
        chatId: Long,
        timestamp: Long,
    ): TypedMessageEntity?

    /**
     * Get all node attachments message id
     *
     * @param chatId
     * @return
     */
    @Query("SELECT messageId FROM typed_messages WHERE chatId = :chatId AND type = :type ORDER BY timestamp DESC")
    suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long>

    /**
     * Get message reactions
     *
     * @param chatId
     * @param msgId
     * @return String with Reactions if any.
     */
    @Query("SELECT reactions FROM typed_messages WHERE chatId = :chatId AND messageId = :msgId")
    suspend fun getMessageReactions(chatId: Long, msgId: Long): String?

    /**
     * Insert message
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param reactions Updated reactions
     */
    @Query("UPDATE typed_messages SET reactions = :reactions WHERE chatId = :chatId AND messageId = :msgId")
    suspend fun updateMessageReactions(chatId: Long, msgId: Long, reactions: String)

    /**
     * Delete messages by id
     *
     * @param messageIds
     */
    @Query("DELETE FROM typed_messages WHERE messageId IN (:messageIds)")
    fun deleteMessagesById(messageIds: List<Long>)

    /**
     * Get msg ids by chat id and latest date
     *
     * @param chatId
     * @param truncateTimestamp
     * @return matching message ids
     */
    @Query("SELECT messageId FROM typed_messages WHERE chatId = :chatId AND timestamp <= :truncateTimestamp")
    fun getMsgIdsByChatIdAndLatestDate(chatId: Long, truncateTimestamp: Long): List<Long>
}