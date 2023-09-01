package mega.privacy.android.feature.sync.data.mapper

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
) {

    operator fun invoke(
        model: MegaSync,
        megaFolderName: String,
        syncStats: MegaSyncStats?,
    ): FolderPair =
        FolderPair(
            id = model.backupId,
            pairName = model.name,
            localFolderPath = model.localFolder,
            remoteFolder = RemoteFolder(model.megaHandle, megaFolderName),
            syncStatus = mapSyncStatus(syncStats, model.runState)
        )
}
