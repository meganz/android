package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * OpenShareDialog use case. This gets called when user shares a node using bottom sheet dialog
 */
fun interface CreateShareKey {

    /**
     * Invoke
     *
     * @param node : [MegaNode]
     */
    suspend operator fun invoke(node: MegaNode)
}