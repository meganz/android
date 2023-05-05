package mega.privacy.android.app.domain.usecase

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
 *  @property getRubbishBinFolder [GetRubbishBinChildren]
 */
class DefaultGetRubbishBinChildren @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getRubbishBinFolder: GetRubbishBinFolder,
    private val addNodeType: AddNodeType,
) : GetRubbishBinChildren {
    override suspend fun invoke(parentHandle: Long): List<TypedNode> {
        val nodeID = if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRubbishBinFolder()?.let {
                NodeId(longValue = it.handle)
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