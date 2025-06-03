package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.shared.resources.R

internal data class SyncUiItem(
    val id: Long,
    val syncType: SyncType,
    val folderPairName: String,
    val status: SyncStatus,
    val hasStalledIssues: Boolean,
    val deviceStoragePath: String,
    val megaStoragePath: String,
    val megaStorageNodeId: NodeId,
    val expanded: Boolean,
    @StringRes val error: Int? = null,
    val deviceStorageUri: UriPath? = null,
    val numberOfFiles: Int = 0,
    val numberOfFolders: Int = 0,
    val totalSizeInBytes: Long = 0,
    val creationTime: Long = 0,
    val isLocalRootChangeNeeded: Boolean = false,
) {
    @get:StringRes
    val method: Int
        get() = when (syncType) {
            SyncType.TYPE_BACKUP -> R.string.sync_add_new_backup_card_sync_type_text
            SyncType.TYPE_CAMERA_UPLOADS -> R.string.general_camera_uploads
            SyncType.TYPE_MEDIA_UPLOADS -> R.string.general_media_uploads
            else -> mega.privacy.android.feature.sync.R.string.sync_two_way
        }
}
