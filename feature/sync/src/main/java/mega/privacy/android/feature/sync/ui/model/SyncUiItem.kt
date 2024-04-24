package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.SyncStatus

internal data class SyncUiItem(
    val id: Long,
    val folderPairName: String,
    val status: SyncStatus,
    val hasStalledIssues: Boolean,
    val deviceStoragePath: String,
    val megaStoragePath: String,
    val megaStorageNodeId: NodeId,
    @StringRes val method: Int,
    val expanded: Boolean,
    @StringRes val error: Int? = null,
    val numberOfFiles: Int = 0,
    val numberOfFolders: Int = 0,
    val totalSizeInBytes: Long = 0,
    val creationTime: Long = 0,
)