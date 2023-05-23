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
     * @param localFingerPrint Fingerprint of the local file
     * @param parentNode       Preferred parent node, could be null for searching all the place in cloud drive
     *
     * @return A node with the same fingerprint, or null when not found
     * */
    suspend operator fun invoke(
        localFingerPrint: String,
        parentNode: NodeId,
    ): TypedFileNode? {

        // Try to find the node by original fingerprint from the selected parent folder
        val nodeListWithParent = getNodeByOriginalFingerprintUseCase(localFingerPrint, parentNode)
        nodeListWithParent?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by fingerprint from the selected parent folder
        val nodeWithParent = getNodeByFingerprintAndParentNodeUseCase(localFingerPrint, parentNode)
        nodeWithParent?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by original fingerprint in the account
        val nodeListNoParent = getNodeByOriginalFingerprintUseCase(localFingerPrint, null)
        nodeListNoParent?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Try to find the node by fingerprint in the account
        val nodeNoParent = getNodeByFingerprintUseCase(localFingerPrint)
        nodeNoParent?.let {
            return addNodeType(it) as? TypedFileNode
        }

        // Node does not exist
        return null
    }
}
