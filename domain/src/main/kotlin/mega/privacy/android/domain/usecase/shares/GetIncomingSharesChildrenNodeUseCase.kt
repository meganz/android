package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import javax.inject.Inject

/**
 * Get the child nodes of the parent handle or the root list of incoming shares nodes.
 *
 * @property getNodeByHandle
 * @property getChildrenNode
 * @property getContactVerificationWarningUseCase
 * @property areCredentialsVerifiedUseCase
 * @property mapNodeToShareUseCase
 * @property getCloudSortOrder
 * @property getOthersSortOrder
 * @property nodeRepository
 */
class GetIncomingSharesChildrenNodeUseCase @Inject constructor(
    private val getNodeByHandle: GetNodeByIdUseCase,
    private val getChildrenNode: GetTypedChildrenNodeUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val mapNodeToShareUseCase: MapNodeToShareUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Get children nodes of the incoming shares parent handle or root list of incoming shares node
     */
    suspend operator fun invoke(parentHandle: Long): List<ShareNode> {
        return if (parentHandle == -1L) {
            val isContactVerificationOn = getContactVerificationWarningUseCase()
            nodeRepository.getAllIncomingShares(getOthersSortOrder()).mapNotNull { shareData ->
                getNodeByHandle(NodeId(shareData.nodeHandle))?.let { node ->
                    runCatching {
                        mapNodeToShareUseCase(node, shareData.let {
                            if (isContactVerificationOn && it.user != null)
                                it.copy(
                                    isContactCredentialsVerified = areCredentialsVerifiedUseCase(it.user)
                                )
                            else it
                        })
                    }.getOrNull()
                }
            }
        } else {
            getNodeByHandle(NodeId(parentHandle))?.let {
                getChildrenNode(it.id, getCloudSortOrder()).map { node ->
                    mapNodeToShareUseCase(node)
                }
            } ?: emptyList()
        }
    }
}