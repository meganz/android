package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for checking if root node exists.
 */
class RootNodeExistsUseCase @Inject constructor(private val nodeRepository: NodeRepository) {

    /**
     * Invoke.
     *
     * @return True if root node exists, false otherwise.
     */
    suspend operator fun invoke() = nodeRepository.getRootNode() != null
}