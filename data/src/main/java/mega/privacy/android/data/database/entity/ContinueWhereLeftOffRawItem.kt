package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo

/**
 * Room POJO for the multi-join query result that powers the
 * "continue where you left off" widget carousel.
 * Not an @Entity — just a data class with @ColumnInfo annotations.
 */
internal data class ContinueWhereLeftOffRawItem(
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "type_name") val typeName: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "last_accessed_timestamp") val lastAccessedTimestamp: Long,
    @ColumnInfo(name = "lastPageViewed") val lastPageViewed: Long?, // camelCase matches the legacy last_page_viewed_in_pdf table schema
    @ColumnInfo(name = "current_position") val currentPosition: Long?,
    @ColumnInfo(name = "total_duration") val totalDuration: Long?,
    @ColumnInfo(name = "cursor_position") val cursorPosition: Int?,
    @ColumnInfo(name = "scroll_spot") val scrollSpot: Float?,
)
