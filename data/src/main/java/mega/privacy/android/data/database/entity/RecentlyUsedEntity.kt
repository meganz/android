package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED

/**
 * Index table for the "continue where you left off" widget carousel.
 * Tracks all recently used items and joins with specialised tables
 * (last_page_viewed_in_pdf, media_playback_info, text_editor_scroll)
 * to build the widget data.
 *
 * @property nodeHandle The node handle of the file.
 * @property typeId The content type ID (FK to recently_used_type).
 * @property fileName The file name, stored on open for fast widget loading.
 * @property lastAccessedTimestamp The timestamp of the last interaction.
 */
@Entity(
    tableName = TABLE_RECENTLY_USED,
    foreignKeys = [
        ForeignKey(
            entity = RecentlyUsedTypeEntity::class,
            parentColumns = ["type_id"],
            childColumns = ["type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["last_accessed_timestamp"], name = "index_recently_used_last_accessed"),
        Index(value = ["type_id"], name = "index_recently_used_type_id"),
    ],
)
internal data class RecentlyUsedEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "type_id") val typeId: Int,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "last_accessed_timestamp") val lastAccessedTimestamp: Long,
)
