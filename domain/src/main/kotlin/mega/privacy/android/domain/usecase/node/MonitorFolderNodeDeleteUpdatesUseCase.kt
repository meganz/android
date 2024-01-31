package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Monitor folder node delete updates for the current logged in user
 */
class MonitorFolderNodeDeleteUpdatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke() = nodeRepository.monitorNodeUpdates()
        .map { nodeUpdate ->
            nodeUpdate.changes.keys.filterIsInstance<FolderNode>()
                .filter { it.isInRubbishBin }
                .map { it.id.longValue }
        }
}
