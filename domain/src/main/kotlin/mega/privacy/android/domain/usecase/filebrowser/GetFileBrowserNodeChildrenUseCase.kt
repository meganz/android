package mega.privacy.android.domain.usecase.filebrowser

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.AddNodesTypeUseCase
import javax.inject.Inject

/**
 * Default get children nodes of the browser parent handle
 *
 *  @property getRootNodeUseCase
 *  @property getCloudSortOrder
 *  @property nodeRepository
 */
class GetFileBrowserNodeChildrenUseCase @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
    private val addNodesTypeUseCase: AddNodesTypeUseCase,
) {

    /**
     * Get children nodes of the browser parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<TypedNode> {
        val nodeId =
            (if (parentHandle != nodeRepository.getInvalidHandle()) NodeId(parentHandle) else getRootNodeUseCase()?.id)
                ?: return emptyList()
        val childNodes =
            nodeRepository.getNodeChildren(nodeId = nodeId, order = getCloudSortOrder())
        return addNodesTypeUseCase(childNodes)
    }
}