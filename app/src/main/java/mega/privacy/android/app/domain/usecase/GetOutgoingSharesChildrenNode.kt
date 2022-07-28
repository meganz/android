package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get children nodes of the outgoing shares parent handle or root list of incoming shares node
 */
fun interface GetOutgoingSharesChildrenNode {
    /**
     * Get a list of all outgoing shares
     *
     * @param parentHandle
     * @return Children nodes of the parent handle
     *         Root list of outgoing shares if parent handle is invalid
     *         null if parent node cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<MegaNode>?
}