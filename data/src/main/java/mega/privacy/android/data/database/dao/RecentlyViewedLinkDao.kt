package mega.privacy.android.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_VIEWED_LINK
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem

/**
 * DAO for the recently_viewed_link table.
 */
@Dao
internal interface RecentlyViewedLinkDao {

    /**
     * Insert or replace a recently viewed link entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLink(entity: RecentlyViewedLinkEntity)

    /**
     * Delete a recently viewed link by node handle.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_VIEWED_LINK WHERE node_handle = :nodeHandle")
    suspend fun deleteByNodeHandle(nodeHandle: Long)

    /**
     * Delete all recently viewed links.
     */
    @Query("DELETE FROM $TABLE_RECENTLY_VIEWED_LINK")
    suspend fun deleteAll()

    /**
     * Paged source over all viewed links, sorted by most recently accessed.
     * Room invalidates this source automatically when the underlying table changes.
     */
    @RawQuery(observedEntities = [RecentlyViewedLinkEntity::class])
    fun getViewedLinksPagingSource(query: SupportSQLiteQuery): PagingSource<Int, ViewedLinkRawItem>
}
