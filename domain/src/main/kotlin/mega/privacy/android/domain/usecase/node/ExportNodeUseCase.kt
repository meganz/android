package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to Export a MegaNode referenced by its [NodeId]
 */
class ExportNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Export a MegaNode referenced by its [NodeId]
     *
     * @param nodeToExport the node's [NodeId] that we want to export
     * @param expireTime the time in seconds since epoch to set as expiry date
     * @return the [String] The link if the request finished with success, error if not
     */
    suspend operator fun invoke(
        nodeToExport: NodeId,
        expireTime: Long? = null,
    ): String = nodeRepository.exportNode(nodeToExport, expireTime)
}