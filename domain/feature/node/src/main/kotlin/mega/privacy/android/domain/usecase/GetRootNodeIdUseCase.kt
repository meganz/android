package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting root node id
 */
class GetRootNodeIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {

    /**
     * Get root node id
     *
     * @return root node id
     */
    suspend operator fun invoke(): NodeId? = nodeRepository.getRootNodeId()
}
