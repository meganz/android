package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}