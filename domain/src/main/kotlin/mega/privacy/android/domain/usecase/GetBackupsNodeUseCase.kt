package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting the Backups node
 */
class GetBackupsNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {

    /**
     * Invocation function
     *
     * @return the Backups Node
     */
    suspend operator fun invoke() = nodeRepository.getBackupsNode()
}