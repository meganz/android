package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity

/**
 * Chat message meta dao
 */
@Dao
interface ChatMessageMetaDao {

    /**
     * Insert rich preview
     *
     * @param richPreview
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRichPreviews(richPreviews: List<RichPreviewEntity>)

    /**
     * Insert giphy
     *
     * @param giphy
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiphys(giphys: List<GiphyEntity>)

    /**
     * Insert geolocation
     *
     * @param geolocation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeolocations(geolocations: List<ChatGeolocationEntity>)

}