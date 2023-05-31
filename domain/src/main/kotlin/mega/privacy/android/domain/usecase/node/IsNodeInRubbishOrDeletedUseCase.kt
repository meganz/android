package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Check if node is in rubbish or deleted
 */
class IsNodeInRubbishOrDeletedUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param nodeHandle
     * @return if node is in rubbish or deleted (null)
     */
    suspend operator fun invoke(nodeHandle: Long): Boolean =
        nodeRepository.isNodeInRubbishOrDeleted(nodeHandle)
}
