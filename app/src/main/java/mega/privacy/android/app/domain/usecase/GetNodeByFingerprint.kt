package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get Mega node by fingerprint only
 */
fun interface GetNodeByFingerprint {
    /**
     * Get MegaNode by fingerprint only
     * @param fingerprint
     * @return MegaNode
     */
    suspend operator fun invoke(fingerprint: String): MegaNode?
}
