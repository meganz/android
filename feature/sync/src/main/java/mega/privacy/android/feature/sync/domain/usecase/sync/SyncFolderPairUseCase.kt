package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Establishes a pair between local and remote directories and starts the syncing process
 */
internal class SyncFolderPairUseCase @Inject constructor(private val syncRepository: SyncRepository) {

    /**
     * Invoke method
     *
     * @param syncType - sync type of the folder pair
     * @param name - name of the folder pair
     * @param localPath - local path on the device
     * @param remotePath - MEGA folder path
     * @return Boolean - indicates whether the folder was set up successfully or not
     */
    suspend operator fun invoke(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remotePath: RemoteFolder
    ): Boolean =
        syncRepository.setupFolderPair(
            syncType = syncType,
            name = name,
            localPath = localPath,
            remoteFolderId = remotePath.id
        )
}
