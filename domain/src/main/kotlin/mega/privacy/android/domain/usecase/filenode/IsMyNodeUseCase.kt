package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to check if current node is my node or not
 * @property nodeRepository [NodeRepository]
 */
class IsMyNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     * @param fileNode [FileNode]
     */
    suspend operator fun invoke(fileNode: FileNode): Boolean {
        val userHandle = nodeRepository.getMyUserHandleBinary()
        return if (userHandle == nodeRepository.getOwnerNode(fileNode.id)) {
            true
        } else {
            fileNode.fingerprint?.let {
                nodeRepository.getNodesFromFingerPrint(it)
                    .any { node ->
                        nodeRepository.getOwnerNode(node.id) == userHandle
                    }
            } ?: false
        }
    }
}