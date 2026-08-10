package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to get the NodeId from a Base64 string
 */
class GetNodeIdFromBase64UseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(base64Node: String): NodeId? =
        nodeRepository.convertBase64ToHandle(base64Node)
            .takeIf { it != nodeRepository.getInvalidHandle() }?.let {
                NodeId(it)
            }
}