package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    fun insert(pendingMessageEntity: PendingMessageEntity): Long

    /**
     * Delete
     *
     * @param id
     */
    @Query("DELETE FROM pending_messages WHERE pendingMessageId = :id")
    fun delete(id: Long)

}