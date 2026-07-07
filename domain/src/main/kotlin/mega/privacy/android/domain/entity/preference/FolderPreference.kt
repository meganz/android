package mega.privacy.android.domain.entity.preference

import mega.privacy.android.domain.entity.SortOrder

/**
 * Per-folder UI preference (device-local).
 *
 * @property folderKey identifies the folder: node-handle-as-string for cloud/shares, file path for offline
 * @property sortOrder the folder's sort order
 * @property viewType the folder's view type
 */
data class FolderPreference(
    val folderKey: String,
    val sortOrder: SortOrder,
    val viewType: ViewType,
)
