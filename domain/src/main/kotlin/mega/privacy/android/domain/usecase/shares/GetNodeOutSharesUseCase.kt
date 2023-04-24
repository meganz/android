package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Gets a list of Contact Permissions for a given node
 */
class GetNodeOutSharesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val contactsRepository: ContactsRepository,
) {

    /**
     * invoke use case
     */
    suspend operator fun invoke(nodeId: NodeId) =
        nodeRepository.getNodeOutgoingShares(nodeId).mapNotNull { shareData ->
            shareData.user?.let { email ->
                contactsRepository.getContactItemFromUserEmail(email, false)?.let {
                    ContactPermission(it, shareData.access)
                }
            }
        }
}