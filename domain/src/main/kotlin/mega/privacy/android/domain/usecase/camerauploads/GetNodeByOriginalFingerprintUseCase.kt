package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Mega node list by original fingerprint
 */
class GetNodeByOriginalFingerprintUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get mega node list
     *
     * @param originalFingerprint
     * @param parent [NodeId]
     * @return [UnTypedNode]
     */
    suspend operator fun invoke(
        originalFingerprint: String,
        parent: NodeId?,
    ) = nodeRepository.getNodeByOriginalFingerprint(originalFingerprint, parent)
}
