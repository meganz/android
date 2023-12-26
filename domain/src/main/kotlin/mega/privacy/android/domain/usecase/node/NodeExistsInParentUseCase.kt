package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to check node is present in existing parent
 * with given name
 */
class NodeExistsInParentUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     * @param node [Node]
     * @param name Name to be searched
     * @return true if same name found else false
     */
    suspend operator fun invoke(node: Node, name: String): Boolean {
        val parentNode = nodeRepository.getParentNode(nodeId = node.id)
        return parentNode?.let {
            val childrenNodes = nodeRepository.getNodeChildren(nodeId = it.id, order = null)
            val searchNode = childrenNodes.find { untypedNode ->
                untypedNode.name == name
            }
            searchNode != null
        } ?: false
    }
}