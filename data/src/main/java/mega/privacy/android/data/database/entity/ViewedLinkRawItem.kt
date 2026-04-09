package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo

/**
 * Room POJO for the multi-join query result that powers the "Viewed Links" screen.
 * Joins recently_used, recently_used_type, and recently_viewed_link tables.
 * Not an @Entity — just a data class with @ColumnInfo annotations.
 */
internal data class ViewedLinkRawItem(
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "type_id") val typeId: Int,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "last_accessed_timestamp") val lastAccessedTimestamp: Long,
    @ColumnInfo(name = "link_url") val linkUrl: String,
)
