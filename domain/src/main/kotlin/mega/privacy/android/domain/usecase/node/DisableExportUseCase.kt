package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to stop sharing a file/folder referenced by its [NodeId]
 */
class DisableExportUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param nodeToDisable the node's [NodeId] to stop sharing
     */
    suspend operator fun invoke(
        nodeToDisable: NodeId,
    ) = nodeRepository.disableExport(nodeToDisable)
}