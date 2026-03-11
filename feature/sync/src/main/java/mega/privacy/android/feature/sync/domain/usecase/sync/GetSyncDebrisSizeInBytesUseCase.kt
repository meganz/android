package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import javax.inject.Inject

/**
 * Use case for getting the total size of all sync debris
 */
internal class GetSyncDebrisSizeInBytesUseCase @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val syncDebrisRepository: SyncDebrisRepository,
) {

    /**
     * Invoke.
     *
     * @return The total size of all sync debris in bytes
     */
    suspend operator fun invoke(): Long =
        getFolderPairsUseCase()
            .takeIf { it.isNotEmpty() }
            ?.let { folderPairs -> syncDebrisRepository.getSyncDebrisForSyncs(folderPairs) }
            ?.sumOf { it.sizeInBytes }
            ?: 0L
}
