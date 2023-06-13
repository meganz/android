package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting root node
 */
class GetRootNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {

    /**
     * Get root node
     *
     * @return root node
     */
    suspend operator fun invoke() = nodeRepository.getUnTypedRootNode()
}