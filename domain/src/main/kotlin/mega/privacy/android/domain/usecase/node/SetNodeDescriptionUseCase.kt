package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to set the description of a node.
 *
 * @property nodeRepository Repository to provide the necessary data.
 */
class SetNodeDescriptionUseCase @Inject constructor(private val nodeRepository: NodeRepository) {

    /**
     * Invoke the use case.
     *
     * @param nodeHandle    Node handle of the node to set the description.
     * @param description   Description to set.
     */
    suspend operator fun invoke(nodeHandle: NodeId, description: String) =
        nodeRepository.setNodeDescription(nodeHandle, description)
}