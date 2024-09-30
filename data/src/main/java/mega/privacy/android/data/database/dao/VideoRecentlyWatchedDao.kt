package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.VideoRecentlyWatchedEntity

@Dao
internal interface VideoRecentlyWatchedDao {
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO}")
    fun getAllRecentlyWatchedVideos(): Flow<List<VideoRecentlyWatchedEntity>>

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO}")
    suspend fun clearRecentlyWatchedVideos()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO} WHERE videoHandle = :handle")
    suspend fun removeRecentlyWatchedVideo(handle: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecentlyWatchedVideo(entity: VideoRecentlyWatchedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecentlyWatchedVideos(entities: List<VideoRecentlyWatchedEntity>)

    @Query("SELECT COUNT(*) FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO}")
    suspend fun getVideoCount(): Int

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO} WHERE watched_timestamp = (SELECT MIN(watched_timestamp) FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO})")
    suspend fun deleteOldestVideo()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO} WHERE videoHandle NOT IN (SELECT videoHandle FROM ${MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO} ORDER BY watched_timestamp DESC LIMIT ${MAX_RECENTLY_WATCHED_VIDEOS})")
    suspend fun deleteExcessVideos()

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(entity: VideoRecentlyWatchedEntity) {
        deleteExcessVideos()
        insertOrUpdateRecentlyWatchedVideo(entity)
        if (getVideoCount() > MAX_RECENTLY_WATCHED_VIDEOS) {
            deleteOldestVideo()
        }
    }

    companion object {
        const val MAX_RECENTLY_WATCHED_VIDEOS = 50
    }
}