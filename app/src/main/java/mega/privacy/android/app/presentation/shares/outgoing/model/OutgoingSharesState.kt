package mega.privacy.android.app.presentation.shares.outgoing.model

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare

/**
 * Outgoing shares UI state
 *
 * @param outgoingHandle current outgoing shares handle
 * @param outgoingTreeDepth current outgoing tree depth
 * @param outgoingParentHandle parent handle of the current outgoing node
 * @param nodes current list of nodes with his shareData associated if unverified or pending
 * @param isInvalidHandle true if handle is invalid
 * @param isLoading true if the nodes are loading
 * @param sortOrder current sort order
 */
data class OutgoingSharesState(
    val outgoingHandle: Long = -1L,
    val outgoingTreeDepth: Int = 0,
    val outgoingParentHandle: Long? = null,
    val nodes: List<Pair<MegaNode, ShareData?>> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {

    /**
     * Check if we are at the root of the outgoing shares page
     *
     * @return true if at the root of the outgoing shares page
     */
    fun isFirstNavigationLevel() = outgoingTreeDepth == 0
}
