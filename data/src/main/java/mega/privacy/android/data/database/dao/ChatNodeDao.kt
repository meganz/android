package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.NodeMessageCrossRef

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

    /**
     * Delete chat nodes by message id
     *
     * @param messageIds
     */
    @Query("DELETE FROM node_message_cross_ref WHERE messageId IN (:messageIds)")
    fun removeMessageNodeRelationship(messageIds: List<Long>)

    /**
     * Delete orphaned nodes
     *
     */
    @Query(
        """
        DELETE FROM chat_node
        WHERE id NOT IN (
            SELECT DISTINCT id FROM node_message_cross_ref
        )
    """
    )
    fun deleteOrphanedNodes()

    /**
     * Insert node message cross ref
     *
     * @param crossRef
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNodeMessageCrossRef(crossRef: NodeMessageCrossRef)
}