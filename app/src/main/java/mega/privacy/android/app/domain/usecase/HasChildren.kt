package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use case for checking if a node has children.
 */
interface HasChildren {

    /**
     * Invoke.
     *
     * @param node  The MegaNode to check.
     * @return True if the node has children, false otherwise.
     */
    suspend operator fun invoke(node: MegaNode): Boolean
}