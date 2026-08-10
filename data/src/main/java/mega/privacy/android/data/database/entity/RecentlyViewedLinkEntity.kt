package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_VIEWED_LINK

@Entity(
    tableName = TABLE_RECENTLY_VIEWED_LINK,
    indices = [
        Index(
            value = ["last_accessed_timestamp"],
            name = "index_recently_viewed_link_last_accessed",
        ),
    ],
)
internal data class RecentlyViewedLinkEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "type_id") val typeId: Int,
    @ColumnInfo(name = "node_name") val nodeName: String,
    @ColumnInfo(name = "link_url") val linkUrl: String,
    @ColumnInfo(name = "last_accessed_timestamp") val lastAccessedTimestamp: Long,
)
