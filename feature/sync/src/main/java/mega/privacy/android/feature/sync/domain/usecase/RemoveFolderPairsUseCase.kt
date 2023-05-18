package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Removes all folder pairs
 */
class RemoveFolderPairsUseCase @Inject constructor(private val syncRepository: SyncRepository) {

    suspend operator fun invoke() {
        syncRepository.removeFolderPairs()
    }
}
