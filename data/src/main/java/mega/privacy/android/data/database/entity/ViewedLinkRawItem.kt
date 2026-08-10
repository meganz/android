package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo

/**
 * Room POJO for the query result that powers the "Viewed Links" screen.
 * Reads self-owned columns from recently_viewed_link and LEFT JOINs
 * recently_used only for type_id.
 * Not an @Entity — just a data class with @ColumnInfo annotations.
 */
internal data class ViewedLinkRawItem(
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "type_id") val typeId: Int,
    @ColumnInfo(name = "node_name") val nodeName: String,
    @ColumnInfo(name = "last_accessed_timestamp") val lastAccessedTimestamp: Long,
    @ColumnInfo(name = "link_url") val linkUrl: String,
)
