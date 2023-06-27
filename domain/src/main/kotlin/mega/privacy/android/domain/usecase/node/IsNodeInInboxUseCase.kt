package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Use Case that returns true when the node is in the inbox.
 */
class IsNodeInInboxUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param handle
     * @return Boolean that determines whether the node is in the inbox or not
     */
    suspend operator fun invoke(handle: Long) = nodeRepository.isNodeInInbox(handle)
}