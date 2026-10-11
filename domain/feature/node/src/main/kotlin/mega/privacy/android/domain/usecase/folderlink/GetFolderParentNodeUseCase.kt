package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Get the parent node
 */
class GetFolderParentNodeUseCase @Inject constructor(
    private val repository: FolderLinkRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Invoke
     *
     * @param nodeId  Handle of the node of which to get the parent
     */
    suspend operator fun invoke(nodeId: NodeId): TypedFolderNode {
        val node = repository.getParentNode(nodeId)
        if (node != null) {
            runCatching { addNodeType(node) as TypedFolderNode }
                .onSuccess { return it }
                .onFailure { throw FetchFolderNodesException.GenericError() }
        }
        throw FetchFolderNodesException.GenericError()
    }
}