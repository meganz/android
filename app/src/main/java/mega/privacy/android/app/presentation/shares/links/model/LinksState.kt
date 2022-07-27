package mega.privacy.android.app.presentation.shares.links.model

import nz.mega.sdk.MegaNode

/**
 * Links shares UI state
 *
 * @param linksParentHandle current links parent handle
 * @param linksTreeDepth current links tree depth
 * @param nodes current list of nodes
 * @param isInvalidParentHandle true if parent handle is invalid
 * @param isLoading true if the nodes are loading
 */
data class LinksState(
    val linksParentHandle: Long = -1L,
    val linksTreeDepth: Int = 0,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidParentHandle: Boolean = true,
    val isLoading: Boolean = false,
) {
    /**
     * Check if we are at the root of the links shares page
     *
     * @return true if at the root of the links shares page
     */
    fun isFirstNavigationLevel() = linksTreeDepth == 0
}