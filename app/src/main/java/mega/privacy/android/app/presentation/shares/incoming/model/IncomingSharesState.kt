package mega.privacy.android.app.presentation.shares.incoming.model

import nz.mega.sdk.MegaNode
import java.util.Stack

/**
 * Incoming shares UI state
 *
 * @param incomingParentHandle current incoming shares parent handle
 * @param incomingTreeDepth current incoming tree depth
 * @param nodes current list of nodes
 * @param lastPositionStack stack of scroll position for each depth
 */
data class IncomingSharesState(
    val incomingParentHandle: Long = -1L,
    val incomingTreeDepth: Int = 0,
    val nodes: List<MegaNode> = emptyList(),
    val lastPositionStack: Stack<Int> = Stack<Int>(),
) {

    /**
     * Check if we are at the root of the incoming shares page
     *
     * @return true if at the root of the incoming shares page
     */
    fun isFirstNavigationLevel() = incomingTreeDepth == 0
}