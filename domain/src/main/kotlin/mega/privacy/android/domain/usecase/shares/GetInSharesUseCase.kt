package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get In Shares UseCase
 *
 * Gets all the nodes shared to the user
 */
class GetInSharesUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Invoke
     *
     * @param email email of the selected user
     * @return [UnTypedNode] list of nodes are returned else empty list returned
     */
    suspend operator fun invoke(email: String): List<UnTypedNode> =
        nodeRepository.getInShares(email)
}