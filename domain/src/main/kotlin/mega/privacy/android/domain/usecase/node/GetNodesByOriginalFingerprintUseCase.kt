package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the list of Nodes having the same Original Fingerprint
 *
 * @param nodeRepository Repository containing all Node-related operations
 */
class GetNodesByOriginalFingerprintUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation function
     *
     * @param originalFingerprint The Original Fingerprint to search for all Nodes
     * @param parentNodeId The parent Node Id. if non-null, then it will only return the list of
     * Nodes under the parent Node. Otherwise, it will search for all Nodes in the account
     *
     * @return The list of Nodes having the same Original Fingerprint under the Parent Node.
     * If no Parent Node is specified, it will return the list of Nodes from the entire account
     */
    suspend operator fun invoke(
        originalFingerprint: String,
        parentNodeId: NodeId?,
    ) = nodeRepository.getNodesByOriginalFingerprint(
        originalFingerprint = originalFingerprint,
        parentNodeId = parentNodeId,
    )
}