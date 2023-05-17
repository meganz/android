package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the list of verified incoming shares
 */
class GetVerifiedIncomingSharesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @return a List of ShareData representing each verified incoming share
     */
    suspend operator fun invoke(order: SortOrder) =
        nodeRepository.getVerifiedIncomingShares(order)
}
