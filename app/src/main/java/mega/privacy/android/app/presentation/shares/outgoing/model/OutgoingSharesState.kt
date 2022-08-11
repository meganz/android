package mega.privacy.android.app.presentation.shares.outgoing.model

import nz.mega.sdk.MegaNode

/**
 * Outgoing shares UI state
 *
 * @param outgoingHandle current outgoing shares handle
 * @param outgoingTreeDepth current outgoing tree depth
 * @param outgoingParentHandle parent handle of the current outgoing node
 * @param nodes current list of nodes
 * @param isInvalidHandle true if handle is invalid
 * @param isLoading true if the nodes are loading
 */
data class OutgoingSharesState(
    val outgoingHandle: Long = -1L,
    val outgoingTreeDepth: Int = 0,
    val outgoingParentHandle: Long? = null,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
) {

    /**
     * Check if we are at the root of the outgoing shares page
     *
     * @return true if at the root of the outgoing shares page
     */
    fun isFirstNavigationLevel() = outgoingTreeDepth == 0
}