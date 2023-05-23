package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get UnTyped node by fingerprint only
 */
class GetNodeByFingerprintUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Get [UnTypedNode] by fingerprint only
     * @param fingerprint
     * @return [UnTypedNode]
     */
    suspend operator fun invoke(fingerprint: String) =
        nodeRepository.getNodeByFingerprint(fingerprint)
}
