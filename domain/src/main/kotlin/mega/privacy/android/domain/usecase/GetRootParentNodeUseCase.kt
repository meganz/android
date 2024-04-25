package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting the root parent node by nodeId
 */
class GetRootParentNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.getRootParentNode(nodeId)
}