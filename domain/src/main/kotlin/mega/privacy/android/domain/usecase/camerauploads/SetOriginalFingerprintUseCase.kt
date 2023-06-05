package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to set the original fingerprint of a [Node]
 */
class SetOriginalFingerprintUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Sets the original fingerprint of a [Node]
     *
     * @param nodeId the [NodeId] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     */
    suspend operator fun invoke(nodeId: NodeId, originalFingerprint: String) =
        nodeRepository.setOriginalFingerprint(nodeId, originalFingerprint)
}
