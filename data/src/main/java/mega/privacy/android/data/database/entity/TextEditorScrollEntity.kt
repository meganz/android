package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_TEXT_EDITOR_SCROLL

/**
 * Stores text editor cursor position and scroll offset for "continue where you left off".
 * Cascade-deletes when the parent recently_used row is pruned or removed.
 *
 * The last accessed timestamp is stored in [RecentlyUsedEntity] (1:1 via node_handle FK)
 * and not duplicated here.
 *
 * @property nodeHandle The node handle of the text file.
 * @property cursorPosition The cursor position (character offset).
 * @property scrollSpot The scroll position (0.0 to 1.0 fraction).
 */
@Entity(
    tableName = TABLE_TEXT_EDITOR_SCROLL,
    foreignKeys = [
        ForeignKey(
            entity = RecentlyUsedEntity::class,
            parentColumns = ["node_handle"],
            childColumns = ["node_handle"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class TextEditorScrollEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_handle") val nodeHandle: Long,
    @ColumnInfo(name = "cursor_position") val cursorPosition: Int,
    @ColumnInfo(name = "scroll_spot") val scrollSpot: Float,
)
