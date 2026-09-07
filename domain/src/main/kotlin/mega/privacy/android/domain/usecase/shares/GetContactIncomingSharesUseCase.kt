package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import javax.inject.Inject

/**
 * Get the root list of folders a given contact shares with the user.
 *
 * @property getNodeByIdUseCase
 * @property getContactVerificationWarningUseCase
 * @property areCredentialsVerifiedUseCase
 * @property mapNodeToShareUseCase
 * @property getOthersSortOrder
 * @property nodeRepository
 */
class GetContactIncomingSharesUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val mapNodeToShareUseCase: MapNodeToShareUseCase,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke.
     *
     * @param email Email of the contact whose incoming shares are requested.
     * @param sortOrder The order to sort by; when null the global others sort order is used.
     * @return List of [ShareNode] the contact shares with the user.
     */
    suspend operator fun invoke(
        email: String,
        sortOrder: SortOrder? = null,
    ): List<ShareNode> {
        val isContactVerificationOn = getContactVerificationWarningUseCase()
        return nodeRepository.getContactIncomingSharesSortedByOrder(
            email = email,
            order = sortOrder ?: getOthersSortOrder(),
        )
            .mapNotNull { shareData ->
                getNodeByIdUseCase(NodeId(shareData.nodeHandle))?.let { node ->
                    runCatching {
                        mapNodeToShareUseCase(
                            node,
                            if (isContactVerificationOn && shareData.user != null) {
                                shareData.copy(
                                    isContactCredentialsVerified = areCredentialsVerifiedUseCase(
                                        shareData.user
                                    )
                                )
                            } else {
                                shareData
                            }
                        )
                    }.getOrNull()
                }
            }
    }
}
