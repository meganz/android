package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get Mega node by fingerprint and parent node
 */
fun interface GetNodeByFingerprintAndParentNode {
    /**
     * Get MegaNode by fingerprint
     * @param fingerprint
     * @param parentNode MegaNode
     * @return MegaNode
     */
    suspend operator fun invoke(fingerprint: String, parentNode: MegaNode?): MegaNode?
}
