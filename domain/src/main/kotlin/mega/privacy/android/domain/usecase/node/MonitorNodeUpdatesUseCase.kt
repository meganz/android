package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Monitor global node updates for the current logged in user
 */
class MonitorNodeUpdatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke() = nodeRepository.monitorNodeUpdates()
}
