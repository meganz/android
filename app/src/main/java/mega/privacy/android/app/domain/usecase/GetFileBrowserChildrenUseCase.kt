package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
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
) {

    /**
     * Get children nodes of the browser parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<Node> {
        val node =
            (if (parentHandle != nodeRepository.getInvalidHandle()) getNodeByHandle(parentHandle) else getRootFolder())
                ?: return emptyList()
        val nodeId = NodeId(node.handle)
        return nodeRepository.getNodeChildren(nodeId = nodeId, order = getCloudSortOrder())
    }
}