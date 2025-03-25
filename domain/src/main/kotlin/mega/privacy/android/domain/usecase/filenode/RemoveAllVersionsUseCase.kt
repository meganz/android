package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to remove all versions of nodes
 */
class RemoveAllVersionsUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * invoke to remove all versions of nodes
     */
    suspend operator fun invoke() {
        nodeRepository.removeAllVersions()
    }
}