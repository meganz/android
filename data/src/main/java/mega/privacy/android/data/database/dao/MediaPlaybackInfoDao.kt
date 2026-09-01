package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaType

@Dao
internal interface MediaPlaybackInfoDao {
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO}")
    fun getAllPlaybackInfos(): Flow<List<MediaPlaybackInfoEntity>>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE media_type = :mediaType")
    fun getAllPlaybackInfosByType(mediaType: MediaType): Flow<List<MediaPlaybackInfoEntity>>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE mediaHandle = :handle")
    suspend fun getMediaPlaybackInfo(handle: Long): MediaPlaybackInfoEntity?

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO}")
    suspend fun clearAllPlaybackInfos()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE media_type = :mediaType")
    suspend fun clearPlaybackInfosByType(mediaType: MediaType)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE mediaHandle = :handle")
    suspend fun removePlaybackInfo(handle: Long)

    /**
     * Delete items beyond the max limit, keeping the most recently updated.
     *
     * `mediaHandle` breaks ties so the surviving set stays defined when rows share a timestamp.
     */
    @Query(
        """
        DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO}
        WHERE mediaHandle NOT IN (
            SELECT mediaHandle FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO}
            ORDER BY updated_at DESC, mediaHandle DESC
            LIMIT :maxItems
        )
        """
    )
    suspend fun deleteExcessItems(maxItems: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaybackInfo(entity: MediaPlaybackInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaybackInfos(entities: List<MediaPlaybackInfoEntity>)

    /**
     * Insert or update a playback info and prune excess items.
     *
     * The position of the playing item is rewritten every few seconds, which replaces an existing
     * row and cannot push the table over the limit. Pruning is therefore skipped unless this write
     * actually adds a row, keeping the periodic saves down to a rowid lookup plus the upsert.
     */
    @Transaction
    suspend fun insertAndPrune(
        entity: MediaPlaybackInfoEntity,
        maxItems: Int = MAX_STORED_PLAYBACK_INFOS,
    ) {
        val isNewRow = getMediaPlaybackInfo(entity.mediaHandle) == null
        insertOrUpdatePlaybackInfo(entity)
        if (isNewRow) deleteExcessItems(maxItems)
    }

    /**
     * Insert or update playback infos in a single transaction and prune excess items.
     */
    @Transaction
    suspend fun insertAndPruneAll(
        entities: List<MediaPlaybackInfoEntity>,
        maxItems: Int = MAX_STORED_PLAYBACK_INFOS,
    ) {
        insertOrUpdatePlaybackInfos(entities)
        deleteExcessItems(maxItems)
    }

    companion object {
        /**
         * Maximum number of media playback positions to retain, counted across audio and video
         * together: both live in this table and pruning does not look at `media_type`, so the
         * oldest position loses regardless of which kind it is. Reaching this cap takes that many
         * distinct media items opened more recently, so in practice only long-abandoned positions
         * are dropped. A bulk import must therefore not stamp its rows with the current time, or
         * the whole batch outranks genuinely recent positions and evicts them.
         *
         * Must stay well above [RecentlyUsedDao.MAX_RECENTLY_USED_ITEMS]: pruning only ever deletes
         * from this table, never from the recently-used index, so a position dropped here while its
         * item is still in the Continue Where Left Off carousel would leave that item resuming from
         * the beginning.
         */
        const val MAX_STORED_PLAYBACK_INFOS = 2000
    }
}
