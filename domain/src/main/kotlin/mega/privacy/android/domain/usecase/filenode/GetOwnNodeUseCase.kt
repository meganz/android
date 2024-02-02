package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case to return Own node from provided node
 * @property nodeRepository [NodeRepository]
 * @property addNodeType [AddNodeType]
 */
class GetOwnNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {

    /**
     * invoke
     * @param fileNode [TypedFileNode]
     * @return [TypedNode]
     */
    suspend operator fun invoke(fileNode: TypedFileNode): TypedNode? {
        val userHandle = nodeRepository.getMyUserHandleBinary()
        return if (userHandle == nodeRepository.getOwnerNodeHandle(fileNode.id)) {
            fileNode
        } else {
            fileNode.fingerprint?.let {
                return nodeRepository.getNodesFromFingerPrint(it).find { node ->
                    (nodeRepository.getOwnerNodeHandle(node.id) == userHandle)
                }?.let { untypedNode ->
                    addNodeType(untypedNode)
                }
            }
        }
    }
}
