package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get parent mega node
 * see @link{mega.privacy.android.domain.usecase.GetParentNodeUseCase} for Domain level implementation
 */
fun interface GetParentMegaNode {
    /**
     * Get parent mega node
     * @param megaNode
     * @return MegaNode
     */
    suspend operator fun invoke(megaNode: MegaNode): MegaNode?
}
