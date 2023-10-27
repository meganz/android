package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to monitor Offline node updates
 */
class MonitorOfflineNodeUpdatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {
    /**
     * Invoke
     */
    operator fun invoke() = nodeRepository.monitorOfflineNodeUpdates()
}