package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Establishes a pair between local and remote directories and starts the syncing process
 */
fun interface SyncFolderPair {

    suspend operator fun invoke(localPath: String, remotePath: RemoteFolder)
}
