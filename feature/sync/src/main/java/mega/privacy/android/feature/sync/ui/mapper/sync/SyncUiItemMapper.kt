package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import javax.inject.Inject


internal class SyncUiItemMapper @Inject constructor(
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper,
    private val syncStatusMapper: SyncStatusMapper,
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

    operator fun invoke(backup: Backup): SyncUiItem =
        SyncUiItem(
            id = backup.backupId,
            syncType = when (backupInfoTypeIntMapper(backup.backupType)) {
                BackupInfoType.CAMERA_UPLOADS -> SyncType.TYPE_CAMERA_UPLOADS
                BackupInfoType.MEDIA_UPLOADS -> SyncType.TYPE_MEDIA_UPLOADS
                else -> SyncType.TYPE_UNKNOWN
            },
            folderPairName = backup.backupName,
            status = syncStatusMapper(backup.state),
            hasStalledIssues = false,
            deviceStoragePath = backup.localFolder,
            megaStoragePath = backup.targetFolderPath,
            megaStorageNodeId = NodeId(backup.targetNode),
            expanded = false,
            error = deviceFolderUINodeErrorMessageMapper(SyncError.NO_SYNC_ERROR)
        )
}