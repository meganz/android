package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Per-folder UI preferences (sort order and view mode), device-local.
 *
 * @property folderKey identifier of the folder: node-handle-as-string for cloud/shares,
 * file path for offline.
 * @property sortOrder the folder's sort order id.
 * @property viewType the folder's view type id.
 */
@Entity(tableName = MegaDatabaseConstant.TABLE_FOLDER_PREFERENCE)
internal data class FolderPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "folder_key")
    val folderKey: String = "",

    // Default 1 = MegaApiJava.ORDER_DEFAULT_ASC (name ascending): the per-folder default sort order.
    @ColumnInfo(name = "sort_order", defaultValue = "1")
    val sortOrder: Int = 1,

    // Default 0 = ViewType.LIST.id: the per-folder default view mode.
    @ColumnInfo(name = "view_type", defaultValue = "0")
    val viewType: Int = 0,
)
