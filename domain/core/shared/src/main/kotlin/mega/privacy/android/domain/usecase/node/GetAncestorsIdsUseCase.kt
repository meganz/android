package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Use case to get all the ids of the ancestors of a node starting for it's parent. The list can be empty if it's the root node.
 */
class GetAncestorsIdsUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Invoke
     */
    suspend operator fun invoke(node: Node): List<NodeId> =
        buildList {
            var parentId: NodeId? = node.parentId
            while (parentId != null && parentId.longValue != -1L) {
                add(parentId)
                parentId = nodeRepository.getParentNodeId(parentId)
            }
        }
}