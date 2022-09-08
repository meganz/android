package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList

/**
 * Get Mega node list by original fingerprint
 */
fun interface GetNodesByOriginalFingerprint {
    /**
     * Get mega node list
     *
     * @param originalFingerprint
     * @param parent MegaNode
     * @return MegaNodeList
     */
    suspend operator fun invoke(
        originalFingerprint: String,
        parent: MegaNode?,
    ): MegaNodeList?
}
