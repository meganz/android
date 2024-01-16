package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.entity.chat.ChatHistoryLoadStatusEntity

/**
 * Chat history state dao
 */
@Dao
interface ChatHistoryStateDao {

    /**
     * Get state
     *
     * @param chatId
     * @return state
     */
    @Query("SELECT * FROM chat_history_load_status WHERE chatId = :chatId")
    suspend fun getState(chatId: Long): ChatHistoryLoadStatusEntity?

    /**
     * Insert state
     *
     * @param state
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: ChatHistoryLoadStatusEntity)

    /**
     * Delete state
     *
     * @param chatId
     */
    @Query("DELETE FROM chat_history_load_status WHERE chatId = :chatId")
    suspend fun deleteState(chatId: Long)
}