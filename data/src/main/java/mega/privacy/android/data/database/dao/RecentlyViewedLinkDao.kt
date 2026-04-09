package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_VIEWED_LINK
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem

/**
 * DAO for the recently viewed link table.
 * Provides CRUD operations and a reactive monitor query that joins
 * recently_viewed_link, recently_used, and recently_used_type tables.
 */
@Dao
internal interface RecentlyViewedLinkDao {

    /**
     * Insert or replace a recently viewed link entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLink(entity: RecentlyViewedLinkEntity)

    /**
     * Insert or replace a recently used entry (parent table).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecentlyUsed(entity: RecentlyUsedEntity)

    /**
     * Save a viewed link by upserting both the parent recently_used row
     * and the child recently_viewed_link row in a single transaction.
     */
    @Transaction
    suspend fun saveViewedLink(
        recentlyUsedEntity: RecentlyUsedEntity,
        recentlyViewedLinkEntity: RecentlyViewedLinkEntity,
    ) {
        insertOrUpdateRecentlyUsed(recentlyUsedEntity)
        insertOrUpdateLink(recentlyViewedLinkEntity)
    }

    /**
     * Delete a recently viewed link by node handle.
     * The parent recently_used row is also deleted because
     * RecentlyViewedLinkEntity has a CASCADE foreign key.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_USED WHERE node_handle = :nodeHandle AND node_handle IN (SELECT node_handle FROM $TABLE_RECENTLY_VIEWED_LINK)")
    suspend fun deleteByNodeHandle(nodeHandle: Long)

    /**
     * Delete all recently viewed links.
     * Deletes the parent recently_used rows for all viewed links,
     * which cascades to the recently_viewed_link child rows.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_USED WHERE node_handle IN (SELECT node_handle FROM $TABLE_RECENTLY_VIEWED_LINK)")
    suspend fun deleteAll()

    /**
     * Monitor all viewed links by joining recently_viewed_link with
     * recently_used and recently_used_type, sorted by most recently accessed.
     */
    @Query(
        """
        SELECT
            ru.node_handle,
            ru.type_id,
            ru.file_name,
            ru.last_accessed_timestamp,
            rvl.link_url
        FROM $TABLE_RECENTLY_VIEWED_LINK rvl
        INNER JOIN $TABLE_RECENTLY_USED ru ON rvl.node_handle = ru.node_handle
        ORDER BY ru.last_accessed_timestamp DESC
        """
    )
    fun monitorViewedLinks(): Flow<List<ViewedLinkRawItem>>
}
