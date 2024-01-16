package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity

/**
 * Chat node dao
 */
@Dao
interface ChatNodeDao {

    /**
     * Insert chat node
     *
     * @param chatNode
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatNodes(chatNodes: List<ChatNodeEntity>)
}