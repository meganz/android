package mega.privacy.android.domain.usecase.filebrowser

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

/**
 * Default get children nodes of the browser parent handle
 *
 *  @property getNodeByHandleUseCase
 *  @property getRootNodeUseCase
 *  @property getCloudSortOrder
 *  @property nodeRepository
 */
class GetFileBrowserNodeChildrenUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
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
            (if (parentHandle != nodeRepository.getInvalidHandle()) getNodeByHandleUseCase(
                parentHandle
            ) else getRootNodeUseCase())
                ?: return emptyList()
        val childNodes =
            nodeRepository.getNodeChildren(nodeId = node.id, order = getCloudSortOrder())
        return childNodes.map { addNodeType(it) }
    }
}