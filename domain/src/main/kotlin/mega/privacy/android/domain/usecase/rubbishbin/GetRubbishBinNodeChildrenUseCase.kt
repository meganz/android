package mega.privacy.android.domain.usecase.rubbishbin

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Default get children nodes of the rubbish bin parent handle
 *
 *  @property nodeRepository [NodeRepository]
 *  @property getCloudSortOrder [GetCloudSortOrder]
 *  @property getRubbishBinFolderUseCase [GetRubbishBinFolderUseCase]
 */
class GetRubbishBinNodeChildrenUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get children nodes of the rubbish bin parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<TypedNode> {
        val nodeID = if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRubbishBinFolderUseCase()?.let {
                NodeId(longValue = it.id.longValue)
            } ?: run {
                return emptyList()
            }
        } else {
            NodeId(longValue = parentHandle)
        }
        val nodes = nodeRepository.getNodeChildren(nodeId = nodeID, order = getCloudSortOrder())
        return nodes.map { addNodeType(it) }
    }
}