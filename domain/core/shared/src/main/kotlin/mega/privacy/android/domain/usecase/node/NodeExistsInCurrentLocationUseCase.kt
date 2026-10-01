package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to check node is present in current location with given name
 */
class NodeExistsInCurrentLocationUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invoke
     *
     * Uses a lightweight name lookup instead of mapping the whole children list,
     * avoiding heavy work when the parent folder has many children.
     *
     * @param nodeId  [NodeId]
     * @param name  Name to be searched
     * @return      True if same name found. False otherwise
     */
    suspend operator fun invoke(nodeId: NodeId, name: String): Boolean =
        nodeRepository.doesChildExistByName(parentNodeId = nodeId, name = name)
}
