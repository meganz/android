package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
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
     * @param node  [Node]
     * @param name  Name to be searched
     * @return      True if same name found. False otherwise
     */
    suspend operator fun invoke(node: Node, name: String): Boolean {
        val childrenNodes = nodeRepository.getNodeChildren(nodeId = node.id, order = null)
        val searchNode = childrenNodes.find { untypedNode ->
            untypedNode.name == name
        }
        return searchNode != null
    }
}