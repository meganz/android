package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case for resuming all syncs
 */
internal class PauseAllSyncsUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke() {
        syncRepository.pauseAllSyncs()
    }
}
