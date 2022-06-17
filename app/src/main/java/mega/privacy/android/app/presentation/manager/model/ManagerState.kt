package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.app.main.DrawerItem

/**
 * Manager UI state
 *
 * @param browserParentHandle current browser parent handle
 * @param rubbishBinParentHandle current rubbish bin parent handle
 * @param incomingParentHandle current incoming parent handle
 * @param outgoingParentHandle current outgoing parent handle
 * @param linksParentHandle current links parent handle
 * @param inboxParentHandle current inbox parent handle
 * @param searchDrawerItem current drawer item set for the search
 * @param searchSharesTab current shared tab set for the search
 * @param isFirstNavigationLevel true if the navigation level is the first level
 */
data class ManagerState(
    val browserParentHandle: Long = -1L,
    val rubbishBinParentHandle: Long = -1L,
    val incomingParentHandle: Long = -1L,
    val outgoingParentHandle: Long = -1L,
    val linksParentHandle: Long = -1L,
    val inboxParentHandle: Long = -1L,
    val searchDrawerItem: DrawerItem? = null,
    val searchSharesTab: SharesTab = SharesTab.NONE,
    val isFirstNavigationLevel: Boolean = true
)