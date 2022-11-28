package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use Case to set the original fingerprint of a [MegaNode]
 */
fun interface SetOriginalFingerprint {

    /**
     * Sets the original fingerprint of a [MegaNode]
     *
     * @param node the [MegaNode] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     */
    suspend operator fun invoke(node: MegaNode, originalFingerprint: String)
}