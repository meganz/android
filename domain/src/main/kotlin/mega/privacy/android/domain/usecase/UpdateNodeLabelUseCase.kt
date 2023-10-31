package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * To change or remove label use case for a node
 * @property nodeRepository [NodeRepository]
 */
class UpdateNodeLabelUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * @param nodeId [NodeId]
     * @param label Int
     */
    suspend operator fun invoke(nodeId: NodeId, label: Int?) {
        label?.let {
            nodeRepository.setNodeLabel(nodeId = nodeId, label = it)
        } ?: run {
            nodeRepository.resetNodeLabel(nodeId = nodeId)
        }
    }
}