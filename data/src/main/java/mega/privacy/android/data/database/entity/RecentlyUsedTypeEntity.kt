package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED_TYPE

/**
 * Lookup table for recently used content types (pdf, video, audio, text_editor).
 * Seeded on database migration.
 *
 * @property typeId The unique identifier for the type.
 * @property name The type name (e.g. "pdf", "video", "audio", "text_editor").
 */
@Entity(
    tableName = TABLE_RECENTLY_USED_TYPE,
    indices = [Index(value = ["name"], unique = true)],
)
internal data class RecentlyUsedTypeEntity(
    @PrimaryKey
    @ColumnInfo(name = "type_id") val typeId: Int,
    @ColumnInfo(name = "name") val name: String,
)
