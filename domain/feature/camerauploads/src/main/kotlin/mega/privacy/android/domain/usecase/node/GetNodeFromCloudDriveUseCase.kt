package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import javax.inject.Inject

/**
 * Use Case that retrieves the Node corresponding to the same fingerprint in Cloud Drive
 *
 * NOTE: only looking for the node by original fingerprint is not enough,
 * because some old nodes do not have the attribute OriginalFingerprint,
 * in that case also look for the node by attribute Fingerprint
 */
class GetNodeFromCloudDriveUseCase @Inject constructor(
    private val getNodesByOriginalFingerprintUseCase: GetNodesByOriginalFingerprintUseCase,
    private val getNodesByFingerprintUseCase: GetNodesByFingerprintUseCase,
) {
    /**
     * @param originalFingerprint Fingerprint of the local file
     * @param generatedFingerprint Generated Fingerprint of the local file
     * @param parentNodeId The Node ID of the Parent Folder
     *
     * @return A Node with the same Fingerprint, or null when not found
     * */
    suspend operator fun invoke(
        originalFingerprint: String,
        generatedFingerprint: String? = null,
        parentNodeId: NodeId,
    ): UnTypedNode? {

        // Check first if the node exists in the Target folder using the original fingerprint
        // If not look for the node in other folders, and select the first one arbitrarily
        getNodesByOriginalFingerprintUseCase(
            originalFingerprint = originalFingerprint,
            parentNodeId = null,
        ).takeIf { it.isNotEmpty() }
            ?.let { nodes ->
                nodes.find { it.parentId == parentNodeId } ?: nodes.firstOrNull()
            }?.let { return it }

        // This covers the case where a Node was uploaded without setting the original fingerprint
        getNodesByFingerprintUseCase(originalFingerprint)
            .takeIf { it.isNotEmpty() }
            ?.let { nodes ->
                nodes.find { it.parentId == parentNodeId } ?: nodes.firstOrNull()
            }?.let { return it }

        // This covers the case where the original fingerprint was not set to the Node attribute
        // For example when the Camera Uploads process was cancelled before the original fingerprint was set
        generatedFingerprint?.let { fingerprint ->
            getNodesByFingerprintUseCase(fingerprint)
                .takeIf { it.isNotEmpty() }
                ?.let { nodes ->
                    nodes.find { it.parentId == parentNodeId } ?: nodes.firstOrNull()
                }?.let { return it }
        }

        // Else, the Node does not exist anywhere
        return null
    }
}
