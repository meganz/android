package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.data.mapper.sync.SyncTypeMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

/**
 * Folder pair mapper that converts from [MegaSync] model to [FolderPair] entity
 */
internal class FolderPairMapper @Inject constructor(
    private val mapSyncStatus: SyncStatusMapper,
    private val syncErrorMapper: SyncErrorMapper,
    private val syncTypeMapper: SyncTypeMapper
) {

    operator fun invoke(
        model: MegaSync,
        megaFolderName: String,
        syncStats: MegaSyncStats?,
        isStorageOverQuota: Boolean,
    ): FolderPair =
        FolderPair(
            id = model.backupId,
            syncType = syncTypeMapper(MegaSync.SyncType.swigToEnum(model.type)),
            pairName = model.name,
            localFolderPath = model.localFolder,
            remoteFolder = RemoteFolder(NodeId(model.megaHandle), megaFolderName),
            syncStatus = mapSyncStatus(
                syncStats = syncStats,
                runningState = model.runState,
                isStorageOverQuota = isStorageOverQuota,
            ),
            syncError = syncErrorMapper(model.error)
        )
}
