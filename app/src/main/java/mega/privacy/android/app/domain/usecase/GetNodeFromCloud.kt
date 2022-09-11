package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get the mega node from cloud if exists
 */
interface GetNodeFromCloud {
    /**
     * Invoke
     *
     * @return mega node
     */
    suspend operator fun invoke(
        localFingerPrint: String,
        parentNode: MegaNode,
    ): MegaNode?
}
