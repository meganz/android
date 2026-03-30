package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_LAST_PAGE_VIEWED_IN_PDF
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED_TYPE
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_TEXT_EDITOR_SCROLL
import mega.privacy.android.data.database.entity.ContinueWhereLeftOffRawItem
import mega.privacy.android.data.database.entity.RecentlyUsedEntity

private const val TYPE_PDF = "pdf"
private const val TYPE_VIDEO = "video"
private const val TYPE_AUDIO = "audio"
private const val TYPE_TEXT_EDITOR = "text_editor"

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
     * Monitor continue where you left off items by joining recently_used
     * with specialised tables (pdf, media, text editor).
     * Returns a reactive Flow sorted by last accessed timestamp.
     */
    @Query(
        """
        SELECT
            ru.node_handle,
            rut.name AS type_name,
            ru.file_name,
            ru.last_accessed_timestamp,
            pdf.lastPageViewed,
            mpi.current_position,
            mpi.total_duration,
            tes.cursor_position,
            tes.scroll_spot
        FROM $TABLE_RECENTLY_USED ru
        INNER JOIN $TABLE_RECENTLY_USED_TYPE rut ON ru.type_id = rut.type_id
        LEFT JOIN $TABLE_LAST_PAGE_VIEWED_IN_PDF pdf
            ON ru.node_handle = pdf.nodeHandle AND rut.name = '$TYPE_PDF'
        LEFT JOIN $TABLE_MEDIA_PLAYBACK_INFO mpi
            ON ru.node_handle = mpi.mediaHandle AND rut.name IN ('$TYPE_VIDEO', '$TYPE_AUDIO')
        LEFT JOIN $TABLE_TEXT_EDITOR_SCROLL tes
            ON ru.node_handle = tes.node_handle AND rut.name = '$TYPE_TEXT_EDITOR'
        ORDER BY ru.last_accessed_timestamp DESC
        LIMIT :limit
        """
    )
    fun monitorContinueWhereLeftOffItems(limit: Int): Flow<List<ContinueWhereLeftOffRawItem>>

    companion object {
        /**
         * Maximum number of recently used items to retain.
         */
        const val MAX_RECENTLY_USED_ITEMS = 50
    }
}
