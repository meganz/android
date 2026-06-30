package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.HomePinnedItemEntity

@Dao
internal interface HomePinnedItemDao {

    /**
     * Monitor all pinned items ordered oldest-pinned first
     */
    @Query("SELECT * FROM home_pinned_item ORDER BY pinned_at ASC")
    fun monitorPinnedItems(): Flow<List<HomePinnedItemEntity>>

    /**
     * Insert or update pinned items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePinnedItems(entities: List<HomePinnedItemEntity>)

    /**
     * Update only the stored name of a pinned item, preserving its handle, folder flag and pin
     * order. Used to refresh the snapshot after the underlying node is renamed.
     */
    @Query("UPDATE home_pinned_item SET node_name = :nodeName WHERE node_handle = :nodeHandle")
    suspend fun updatePinnedItemName(nodeHandle: Long, nodeName: String)

    /**
     * Delete a pinned item by its node handle
     */
    @Query("DELETE FROM home_pinned_item WHERE node_handle = :nodeHandle")
    suspend fun deletePinnedItemByHandle(nodeHandle: Long)

    /**
     * Delete all pinned items
     */
    @Query("DELETE FROM home_pinned_item")
    suspend fun deleteAllPinnedItems()
}
