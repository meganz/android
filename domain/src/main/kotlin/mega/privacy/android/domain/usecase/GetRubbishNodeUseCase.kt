package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting rubbish node
 */
class GetRubbishNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {

    /**
     * Get rubbish node
     *
     * @return rubbish node
     */
    suspend operator fun invoke() = nodeRepository.getRubbishNode()
}