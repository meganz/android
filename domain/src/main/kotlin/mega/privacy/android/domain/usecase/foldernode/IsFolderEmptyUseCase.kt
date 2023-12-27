package mega.privacy.android.domain.usecase.foldernode

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Returns the folder empty
 */
class IsFolderEmptyUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     *
     * @param node [Node]
     * @return true if folder is empty false otherwise
     */
    suspend operator fun invoke(node: Node): Boolean {
        if (node is FileNode) return false
        if (node is FolderNode) {
            val childNodes = nodeRepository.getNodeChildren(nodeId = node.id, order = null)
            if (childNodes.isNotEmpty()) {
                childNodes.forEach {
                    if (it is FileNode || invoke(it).not()) return false
                }
            }
        }
        return true
    }
}