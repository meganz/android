package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Returns all setup folder pairs.
 */
class GetFolderPairsUseCase @Inject constructor(private val syncRepository: SyncRepository) {

    suspend operator fun invoke(): List<FolderPair> =
        syncRepository.getFolderPairs()
}
