package mega.privacy.android.feature.sync.ui.model

import mega.privacy.android.feature.sync.domain.entity.SyncStatus

internal data class SyncUiItem(
    val id: Long,
    val folderPairName: String,
    val status: SyncStatus,
    val hasStalledIssues: Boolean,
    val deviceStoragePath: String,
    val megaStoragePath: String,
    val method: String,
    val expanded: Boolean,
)