package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Home pinned item entity. [nodeName] and [isFolder] are a snapshot for fast rendering.
 */
@Entity(tableName = MegaDatabaseConstant.TABLE_HOME_PINNED_ITEM)
internal data class HomePinnedItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_handle")
    val nodeHandle: Long,
    @ColumnInfo(name = "node_name")
    val nodeName: String,
    @ColumnInfo(name = "is_folder")
    val isFolder: Boolean,
    @ColumnInfo(name = "pinned_at")
    val pinnedAt: Long,
)
