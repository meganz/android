package mega.privacy.android.app.presentation.shares.outgoing.model

import nz.mega.sdk.MegaNode

/**
 * Outgoing shares UI state
 *
 * @param outgoingParentHandle current outgoing shares parent handle
 * @param outgoingTreeDepth current outgoing tree depth
 * @param nodes current list of nodes
 * @param isInvalidParentHandle true if parent handle is invalid
 * @param isLoading true if the nodes are loading
 */
data class OutgoingSharesState(
    val outgoingParentHandle: Long = -1L,
    val outgoingTreeDepth: Int = 0,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidParentHandle: Boolean = true,
    val isLoading: Boolean = false,
) {

    /**
     * Check if we are at the root of the outgoing shares page
     *
     * @return true if at the root of the outgoing shares page
     */
    fun isFirstNavigationLevel() = outgoingTreeDepth == 0
}