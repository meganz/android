package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.feature.sync.data.mock.MegaSync
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import javax.inject.Inject

/**
 * Folder pair mapper that converts from [MegaSync] model to [FolderPair] entity
 */
internal class FolderPairMapper @Inject constructor() {

    operator fun invoke(
        model: MegaSync,
        megaFolderName: String,
    ): FolderPair =
        FolderPair(
            id = model.backupId,
            pairName = model.name,
            localFolderPath = model.localFolder,
            remoteFolder = RemoteFolder(model.megaHandle, megaFolderName),
            state = FolderPairState.getByOrdinal(model.runState)
        )
}
