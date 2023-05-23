package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Mega node by fingerprint and parent node
 */
class GetNodeByFingerprintAndParentNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get [UnTypedNode] by fingerprint
     * @param fingerprint
     * @param parentNode [NodeId]
     * @return [UnTypedNode]
     */
    suspend operator fun invoke(fingerprint: String, parentNode: NodeId) =
        nodeRepository.getNodeByFingerprintAndParentNode(fingerprint, parentNode)
}
