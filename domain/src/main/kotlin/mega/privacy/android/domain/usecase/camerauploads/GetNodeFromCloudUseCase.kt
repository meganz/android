package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Check if there is a node with the same fingerprint in cloud drive in order to avoid uploading duplicate files
 *
 * NOTE: only looking for the node by original fingerprint is not enough,
 * because some old nodes do not have the attribute OriginalFingerprint,
 * in that case also look for the node by attribute Fingerprint
 */
class GetNodeFromCloudUseCase @Inject constructor(
    private val getNodeByFingerprintUseCase: GetNodeByFingerprintUseCase,
    private val getNodeByOriginalFingerprintUseCase: GetNodeByOriginalFingerprintUseCase,
    private val getNodeByFingerprintAndParentNodeUseCase: GetNodeByFingerprintAndParentNodeUseCase,
    private val addNodeType: AddNodeType,
) {
    /**
     * @param originalFingerprint Fingerprint of the local file
     * @param generatedFingerprint Generated fingerprint of the local file
     * @param parentNodeId Preferred parent node, could be null for searching all the place in cloud drive
     *
     * @return A node with the same fingerprint, or null when not found
     * */
    suspend operator fun invoke(
        originalFingerprint: String,
        generatedFingerprint: String? = null,
        parentNodeId: NodeId,
    ): TypedFileNode? {

        // Try to find the node by original fingerprint from the selected parent folder
        getNodeByOriginalFingerprintUseCase(originalFingerprint, parentNodeId)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by original fingerprint in the account
        getNodeByOriginalFingerprintUseCase(originalFingerprint, null)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by original fingerprint from the selected parent folder
        getNodeByFingerprintAndParentNodeUseCase(originalFingerprint, parentNodeId)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by original fingerprint in the account
        getNodeByFingerprintUseCase(originalFingerprint)?.let {
            return addNodeType(it) as? TypedFileNode
        }

        generatedFingerprint?.let { fingerprint ->
            // Try to find the node by generated fingerprint from the selected parent folder
            getNodeByFingerprintAndParentNodeUseCase(fingerprint, parentNodeId)?.let {
                return addNodeType(it) as? TypedFileNode
            }

            // Try to find the node by generated fingerprint in the account
            getNodeByFingerprintUseCase(fingerprint)?.let {
                return addNodeType(it) as? TypedFileNode
            }
        }

        // Node does not exist
        return null
    }
}
