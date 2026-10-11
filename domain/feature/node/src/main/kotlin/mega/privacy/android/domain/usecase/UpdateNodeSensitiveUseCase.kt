package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

class UpdateNodeSensitiveUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    suspend operator fun invoke(nodeId: NodeId, isSensitive: Boolean) {
        nodeRepository.updateNodeSensitive(nodeId = nodeId, isSensitive = isSensitive)
    }
}