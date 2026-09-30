package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to start offline sync worker
 */
class StartOfflineSyncWorkerUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = nodeRepository.startOfflineSyncWorker()
}