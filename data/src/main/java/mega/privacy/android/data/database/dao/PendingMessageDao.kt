package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity

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
     * Update
     *
     * @param pendingMessageEntity
     */
    @Update
    suspend fun update(pendingMessageEntity: PendingMessageEntity)

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
     * Fetch pending messages for chat
     *
     * @param chatId
     * @return flow of pending messages for a chat
     */
    @Query("SELECT * FROM pending_messages WHERE chatId = :chatId")
    fun fetchPendingMessagesForChat(chatId: Long): Flow<List<PendingMessageEntity>>

}