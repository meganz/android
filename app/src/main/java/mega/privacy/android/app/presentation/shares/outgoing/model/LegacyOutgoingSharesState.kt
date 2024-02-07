package mega.privacy.android.app.presentation.shares.outgoing.model

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * Outgoing shares UI state
 *
 * @property currentViewType serves as the original View Type
 * @property outgoingHandle current outgoing shares handle
 * @property outgoingTreeDepth current outgoing tree depth
 * @property outgoingParentHandle parent handle of the current outgoing node
 * @property nodes current list of nodes with his shareData associated if unverified or pending
 * @property isInvalidHandle true if handle is invalid
 * @property isLoading true if the nodes are loading
 * @property sortOrder current sort order
 */
data class LegacyOutgoingSharesState(
    val currentViewType: ViewType = ViewType.LIST,
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
