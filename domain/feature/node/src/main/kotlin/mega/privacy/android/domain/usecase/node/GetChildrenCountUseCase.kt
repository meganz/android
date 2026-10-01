package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Number of direct children of a folder, files and folders together, regardless of any sensitivity
 * filter
 *
 * @property nodeRepository
 */
class GetChildrenCountUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodeId the folder
     * @return the count, or 0 when the node does not exist or is a file
     */
    suspend operator fun invoke(nodeId: NodeId): Int = nodeRepository.getChildrenCount(nodeId)
}
