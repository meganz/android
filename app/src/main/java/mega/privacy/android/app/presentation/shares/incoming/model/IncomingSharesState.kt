package mega.privacy.android.app.presentation.shares.incoming.model

import nz.mega.sdk.MegaNode

/**
 * Incoming shares UI state
 *
 * @param incomingParentHandle current incoming shares parent handle
 * @param incomingTreeDepth current incoming tree depth
 * @param nodes current list of nodes
 * @param isInvalidParentHandle true if parent handle is invalid
 * @param isLoading true if the nodes are loading
 */
data class IncomingSharesState(
    val incomingParentHandle: Long = -1L,
    val incomingTreeDepth: Int = 0,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidParentHandle: Boolean = true,
    val isLoading: Boolean = false,
) {

    /**
     * Check if we are at the root of the incoming shares page
     *
     * @return true if at the root of the incoming shares page
     */
    fun isFirstNavigationLevel() = incomingTreeDepth == 0
}