package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the Backups node of the account
 */
class GetBackupsNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation function
     *
     * @return the Backups node, or null if it cannot be retrieved
     */
    suspend operator fun invoke() = nodeRepository.getBackupsNode()
}