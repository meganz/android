package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import javax.inject.Inject

internal class SyncUiItemMapper @Inject constructor(
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
) {

    operator fun invoke(folderPairs: List<FolderPair>): List<SyncUiItem> =
        folderPairs.map { invoke(it) }

    operator fun invoke(folderPair: FolderPair): SyncUiItem =
        SyncUiItem(
            id = folderPair.id,
            syncType = folderPair.syncType,
            folderPairName = folderPair.pairName,
            status = folderPair.syncStatus,
            hasStalledIssues = false,
            deviceStoragePath = folderPair.localFolderPath,
            megaStoragePath = folderPair.remoteFolder.name,
            megaStorageNodeId = NodeId(folderPair.remoteFolder.id),
            expanded = false,
            error = deviceFolderUINodeErrorMessageMapper(folderPair.syncError)
        )
}