package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    fun getMediaPlaybackInfo(handle: Long): MediaPlaybackInfoEntity?

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO}")
    suspend fun clearAllPlaybackInfos()

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE media_type = :mediaType")
    suspend fun clearPlaybackInfosByType(mediaType: MediaType)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO} WHERE mediaHandle = :handle")
    suspend fun removePlaybackInfo(handle: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaybackInfo(entity: MediaPlaybackInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaybackInfos(entities: List<MediaPlaybackInfoEntity>)
}