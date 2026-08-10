package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import javax.inject.Inject

/**
 * Get children nodes of the outgoing shares parent handle or root list of outgoing shares node
 *
 * @property getNodeByHandle
 * @property getChildrenNode
 * @property nodeRepository
 */
class GetOutgoingSharesChildrenNodeUseCase @Inject constructor(
    private val getNodeByHandle: GetNodeByIdUseCase,
    private val getChildrenNode: GetTypedChildrenNodeUseCase,
    private val mapNodeToShareUseCase: MapNodeToShareUseCase,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get children nodes of the outgoing shares parent handle or root list of outgoing shares node
     * @param parentHandle
     * @param sortOrder the order to sort by.
     */
    suspend operator fun invoke(
        parentHandle: Long,
        sortOrder: SortOrder,
    ): List<ShareNode> {
        return if (parentHandle == -1L) {
            nodeRepository.getAllOutgoingShares(
                order = sortOrder,
            ).mapNotNull { shareData ->
                getNodeByHandle(NodeId(shareData.nodeHandle))?.let { node ->
                    runCatching {
                        mapNodeToShareUseCase(node, shareData)
                    }.getOrNull()
                }
            }
        } else {
            getNodeByHandle(NodeId(parentHandle))?.let {
                getChildrenNode(it.id, sortOrder).map { node ->
                    mapNodeToShareUseCase(node)
                }
            } ?: emptyList()
        }
    }
}