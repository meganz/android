package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get parent mega node
 */
fun interface GetParentMegaNode {
    /**
     * Get parent mega node
     * @param megaNode
     * @return MegaNode
     */
    suspend operator fun invoke(megaNode: MegaNode): MegaNode?
}
