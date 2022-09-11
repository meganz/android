package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import timber.log.Timber
import javax.inject.Inject

/**
 * Check if there is a node with the same fingerprint in cloud drive in order to avoid uploading duplicate files
 *
 * NOTE: only looking for the node by original fingerprint is not enough,
 * because some old nodes do not have the attribute OriginalFingerprint,
 * in that case also look for the node by attribute Fingerprint
 */
class DefaultGetNodeFromCloud @Inject constructor(
    private val getNodeByFingerprint: GetNodeByFingerprint,
    private val getNodesByOriginalFingerprint: GetNodesByOriginalFingerprint,
    private val getNodeByFingerprintAndParentNode: GetNodeByFingerprintAndParentNode,
) : GetNodeFromCloud {
    /**
     * @param localFingerPrint Fingerprint of the local file
     * @param parentNode       Preferred parent node, could be null for searching all the place in cloud drive
     *
     * @return A node with the same fingerprint, or null when not found
     * */
    override suspend fun invoke(
        localFingerPrint: String,
        parentNode: MegaNode,
    ): MegaNode? {

        // Try to find the node by original fingerprint from the selected parent folder
        val nodeListWithParent = getNodesByOriginalFingerprint(localFingerPrint, parentNode)
        nodeListWithParent.firstNodeOrNull()?.let {
            Timber.d("Found node by original fingerprint with the same local fingerprint in node with handle: ${parentNode.handle}, node handle: ${it.handle}")
            return it
        }

        // Try to find the node by fingerprint from the selected parent folder
        val nodeWithParent = getNodeByFingerprintAndParentNode(localFingerPrint, parentNode)
        nodeWithParent?.let {
            Timber.d("Found node by fingerprint with the same local fingerprint in node with handle: ${parentNode.handle}, node handle: ${it.handle}")
            return it
        }

        // Try to find the node by original fingerprint in the account
        val nodeListNoParent = getNodesByOriginalFingerprint(localFingerPrint, null)
        nodeListNoParent.firstNodeOrNull()?.let {
            Timber.d("Found node by original fingerprint with the same local fingerprint in the account, node handle: ${it.handle}")
            return it
        }

        // Try to find the node by fingerprint in the account
        val nodeNoParent = getNodeByFingerprint(localFingerPrint)
        nodeNoParent?.let {
            Timber.d("Found node by fingerprint with the same local fingerprint in the account, node handle: ${it.handle}")
            return it
        }

        // Node does not exist
        return null
    }

    private fun MegaNodeList?.firstNodeOrNull(): MegaNode? =
        if (this != null && this.size() > 0) this[0] else null
}
