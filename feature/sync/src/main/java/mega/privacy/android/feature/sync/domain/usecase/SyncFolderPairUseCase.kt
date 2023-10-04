package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Establishes a pair between local and remote directories and starts the syncing process
 */
internal class SyncFolderPairUseCase @Inject constructor(private val syncRepository: SyncRepository) {

    suspend operator fun invoke(
        name: String?,
        localPath: String,
        remotePath: RemoteFolder
    ): Boolean =
        syncRepository.setupFolderPair(
            name,
            localPath,
            remotePath.id
        )
}
