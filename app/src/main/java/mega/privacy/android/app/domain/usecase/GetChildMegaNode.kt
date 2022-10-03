package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get child mega node
 */
fun interface GetChildMegaNode {
    /**
     * Get the child node with the provided name
     *
     * @param parentNode
     * @param name
     * @return mega node or null if doesn't exist
     */
    suspend operator fun invoke(parentNode: MegaNode?, name: String?): MegaNode?
}
