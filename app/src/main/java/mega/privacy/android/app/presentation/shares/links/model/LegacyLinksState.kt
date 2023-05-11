package mega.privacy.android.app.presentation.shares.links.model

import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaNode

/**
 * Links shares UI state
 *
 * @param linksHandle current links handle
 * @param linksTreeDepth current links tree depth
 * @param linksParentHandle parent handle of the current link node
 * @param nodes current list of nodes
 * @param isInvalidHandle true if parent handle is invalid
 * @param isLoading true if the nodes are loading
 * @param sortOrder current sort order
 */
data class LegacyLinksState(
    val linksHandle: Long = -1L,
    val linksTreeDepth: Int = 0,
    val linksParentHandle: Long? = null,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    /**
     * Check if we are at the root of the links shares page
     *
     * @return true if at the root of the links shares page
     */
    fun isFirstNavigationLevel() = linksTreeDepth == 0
}