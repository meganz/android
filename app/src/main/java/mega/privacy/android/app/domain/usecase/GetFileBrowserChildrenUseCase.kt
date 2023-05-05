package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Default get children nodes of the browser parent handle
 *
 *  @property getNodeByHandle
 *  @property getRootFolder
 *  @property getCloudSortOrder
 *  @property nodeRepository
 */
class GetFileBrowserChildrenUseCase @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getRootFolder: GetRootFolder,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {

    /**
     * Get children nodes of the browser parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<TypedNode> {
        val node =
            (if (parentHandle != nodeRepository.getInvalidHandle()) getNodeByHandle(parentHandle) else getRootFolder())
                ?: return emptyList()
        val nodeId = NodeId(node.handle)
        val childNodes =
            nodeRepository.getNodeChildren(nodeId = nodeId, order = getCloudSortOrder())
        return childNodes.map { addNodeType(it) }
    }
}