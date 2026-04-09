package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED
import mega.privacy.android.data.database.entity.RecentlyUsedEntity

/**
 * DAO for the recently used index table.
 */
@Dao
internal interface RecentlyUsedDao {
    /**
     * Insert or update a recently used item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: RecentlyUsedEntity)

    /**
     * Get a recently used item by node handle.
     */
    @Query("SELECT * FROM $TABLE_RECENTLY_USED WHERE node_handle = :nodeHandle")
    suspend fun getByNodeHandle(nodeHandle: Long): RecentlyUsedEntity?

    /**
     * Delete a recently used item by node handle.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_USED WHERE node_handle = :nodeHandle")
    suspend fun deleteByNodeHandle(nodeHandle: Long)

    /**
     * Delete all recently used items.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_USED")
    suspend fun deleteAll()

    /**
     * Get the count of recently used items.
     */
    @Query("SELECT COUNT(*) FROM $TABLE_RECENTLY_USED")
    suspend fun getCount(): Int

    /**
     * Delete items beyond the max limit, keeping the most recent.
     */
    @Query(
        """
        DELETE FROM $TABLE_RECENTLY_USED
        WHERE node_handle NOT IN (
            SELECT node_handle FROM $TABLE_RECENTLY_USED
            ORDER BY last_accessed_timestamp DESC
            LIMIT :maxItems
        )
        """
    )
    suspend fun deleteExcessItems(maxItems: Int)

    /**
     * Insert or update a recently used item and prune excess items.
     */
    @Transaction
    suspend fun insertAndPrune(
        entity: RecentlyUsedEntity,
        maxItems: Int = MAX_RECENTLY_USED_ITEMS,
    ) {
        insertOrUpdate(entity)
        deleteExcessItems(maxItems)
    }

    /**
     * Monitor recently used items sorted by last accessed timestamp descending.
     */
    @Query(
        """
        SELECT * FROM $TABLE_RECENTLY_USED
        ORDER BY last_accessed_timestamp DESC
        LIMIT :limit
        """
    )
    fun monitorRecentlyUsedItems(limit: Int): Flow<List<RecentlyUsedEntity>>

    companion object {
        /**
         * Maximum number of recently used items to retain.
         */
        const val MAX_RECENTLY_USED_ITEMS = 50
    }
}
