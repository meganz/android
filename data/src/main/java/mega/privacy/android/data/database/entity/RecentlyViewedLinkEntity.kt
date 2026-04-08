package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_VIEWED_LINK

@Entity(
    tableName = TABLE_RECENTLY_VIEWED_LINK,
    foreignKeys = [
        ForeignKey(
            entity = RecentlyUsedEntity::class,
            parentColumns = ["node_handle"],
            childColumns = ["node_handle"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
internal data class RecentlyViewedLinkEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "link_url") val linkUrl: String,
)