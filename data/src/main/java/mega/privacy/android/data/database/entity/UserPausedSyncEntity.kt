package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Entity for [MegaDatabaseConstant.TABLE_USER_PAUSED_SYNCS]
 */
@Entity(MegaDatabaseConstant.TABLE_USER_PAUSED_SYNCS)
data class UserPausedSyncEntity(

    /**
     * Sync id
     */
    @PrimaryKey
    @ColumnInfo(name = "sync_id")
    val syncId: Long,
)