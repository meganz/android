package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import javax.inject.Inject

/**
 * Get children nodes of the outgoing shares parent handle or root list of outgoing shares node
 *
 * @property getNodeByHandle
 * @property getChildrenNode
 * @property getCloudSortOrder
 * @property getOthersSortOrder
 * @property nodeRepository
 */
class GetOutgoingSharesChildrenNodeUseCase @Inject constructor(
    private val getNodeByHandle: GetNodeByIdUseCase,
    private val getChildrenNode: GetTypedChildrenNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get children nodes of the outgoing shares parent handle or root list of outgoing shares node
     */
    suspend operator fun invoke(parentHandle: Long): List<ShareNode>? {
        return if (parentHandle == -1L) {
            nodeRepository.getAllOutgoingShares(getOthersSortOrder()).mapNotNull { shareData ->
                getNodeByHandle(NodeId(shareData.nodeHandle))?.let { node ->
                    ShareNode(node, shareData)
                }
            }
        } else {
            getNodeByHandle(NodeId(parentHandle))?.let {
                getChildrenNode(it.id, getCloudSortOrder()).map { node ->
                    ShareNode(node)
                }
            }
        }
    }
}