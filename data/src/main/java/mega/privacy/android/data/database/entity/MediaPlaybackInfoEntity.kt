package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.mediaplayer.MediaType

/**
 * Entity representing media playback information in the database.
 *
 * @property mediaHandle The unique identifier for the media item.
 * @property totalDuration The total duration of the media in milliseconds.
 * @property currentPosition The current playback position in milliseconds.
 * @property mediaType The type of media (e.g., audio, video).
 * @property updatedAt Epoch millis of the last write, so positions can be conflict-resolved
 * if they are ever synced across devices. Rows migrated from before this column carry 0.
 */
@Entity(tableName = MegaDatabaseConstant.TABLE_MEDIA_PLAYBACK_INFO)
data class MediaPlaybackInfoEntity(
    @PrimaryKey
    val mediaHandle: Long,
    @ColumnInfo(name = "total_duration", defaultValue = "0")
    val totalDuration: Long = 0L,
    @ColumnInfo(name = "current_position", defaultValue = "0")
    val currentPosition: Long = 0L,
    @ColumnInfo(name = "media_type")
    val mediaType: MediaType,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
)
