package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Does Node Exist Use Case
 *
 */
class DoesNodeExistUseCase @Inject constructor(
    private val repository: NodeRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(nodeId: NodeId) = repository.doesNodeExist(nodeId)
}