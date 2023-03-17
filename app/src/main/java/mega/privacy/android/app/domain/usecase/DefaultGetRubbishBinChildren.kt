package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
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
) : GetRubbishBinChildren {
    override suspend fun invoke(parentHandle: Long): List<Node> {
        val nodeID = if (parentHandle == nodeRepository.getInvalidHandle()) {
            getRubbishBinFolder()?.let {
                NodeId(longValue = parentHandle)
            } ?: run {
                return emptyList()
            }
        } else {
            NodeId(longValue = parentHandle)
        }
        return nodeRepository.getNodeChildren(nodeId = nodeID, order = getCloudSortOrder())
    }
}