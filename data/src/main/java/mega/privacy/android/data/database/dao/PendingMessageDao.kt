package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndNodeHandleRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest

/**
 * Pending message dao
 */
@Dao
interface PendingMessageDao {

    /**
     * Insert
     *
     * @param pendingMessageEntity
     * @return id of the inserted row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingMessageEntity: PendingMessageEntity): Long

    /**
     * Insert multiple
     *
     * @param pendingMessageEntities
     * @return list of ids of the inserted rows
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingMessageEntities: List<PendingMessageEntity>): List<Long>

    /**
     * Update the pending message
     *
     * @param updatePendingMessageStateRequest
     */
    @Update(entity = PendingMessageEntity::class)
    suspend fun update(updatePendingMessageStateRequest: UpdatePendingMessageStateRequest)

    /**
     * Update the pending message
     *
     * @param updatePendingMessageStateAndNodeHandleRequest
     */
    @Update(entity = PendingMessageEntity::class)
    suspend fun update(updatePendingMessageStateAndNodeHandleRequest: UpdatePendingMessageStateAndNodeHandleRequest)

    /**
     * Update the pending message
     *
     * @param updatePendingMessageTransferTagRequest
     */
    @Update(entity = PendingMessageEntity::class)
    suspend fun update(updatePendingMessageTransferTagRequest: UpdatePendingMessageTransferTagRequest)

    /**
     * Delete
     *
     * @param id
     */
    @Query("DELETE FROM pending_messages WHERE pendingMessageId = :id")
    suspend fun delete(id: Long)

    /**
     * Get
     *
     * @param id
     */

    @Query("SELECT * FROM pending_messages WHERE pendingMessageId = :id")
    suspend fun get(id: Long): PendingMessageEntity?

    /**
     * Get all pending messages in a specific state
     */
    @Query("SELECT * FROM pending_messages where state = :state")
    suspend fun getByState(state: PendingMessageState): List<PendingMessageEntity>

    /**
     * Fetch pending messages for chat
     *
     * @param chatId
     * @return flow of pending messages for a chat
     */
    @Query("SELECT * FROM pending_messages WHERE chatId = :chatId")
    fun fetchPendingMessagesForChat(chatId: Long): Flow<List<PendingMessageEntity>>


    /**
     * Delete
     *
     * @param chatId
     */
    @Query("DELETE FROM pending_messages WHERE chatId = :chatId")
    suspend fun deleteAllForChat(chatId: Long)
}