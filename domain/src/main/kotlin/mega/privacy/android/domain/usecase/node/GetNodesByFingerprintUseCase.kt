package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to retrieve all existing Nodes having the same Fingerprint from the entire account
 *
 * @param nodeRepository Repository containing all Node-related operations
 */
class GetNodesByFingerprintUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation function
     *
     * @param fingerprint The Fingerprint to search for all Nodes
     *
     * @return The list of Nodes having the same Fingerprint from the entire account
     */
    suspend operator fun invoke(fingerprint: String) =
        nodeRepository.getNodesByFingerprint(fingerprint)
}