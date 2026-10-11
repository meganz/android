package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to check if node is sensitive inherited
 */
class HasSensitiveInheritedUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(nodeId: NodeId): Boolean =
        nodeRepository.hasSensitiveInherited(nodeId)
}
