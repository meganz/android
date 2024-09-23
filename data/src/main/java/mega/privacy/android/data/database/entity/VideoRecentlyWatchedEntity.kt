package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

@Entity(tableName = MegaDatabaseConstant.TABLE_RECENTLY_WATCHED_VIDEO)
internal data class VideoRecentlyWatchedEntity(
    @PrimaryKey
    val videoHandle: Long = 0L,
    @ColumnInfo(name = "watched_timestamp")
    val watchedTimestamp: Long = 0L,
)
