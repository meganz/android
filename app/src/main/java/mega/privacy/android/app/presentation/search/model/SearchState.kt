package mega.privacy.android.app.presentation.search.model

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * Search UI state
 *
 * @param nodes nodes retrieved from a search request, null if not set yet
 * @param searchParentHandle current search parent handle
 * @param searchDrawerItem current drawer item set for the search
 * @param searchSharesTab current shared tab set for the search
 * @param textSubmitted flag to control if a search has been performed
 *                      It is also used to prevent a search to be performed
 *                      if the user is not manually triggering it
 * @param searchQuery current search query
 * @param searchDepth current search depth count
 * @param isInProgress current progress state of the search request
 * @param isMandatoryFingerPrintVerificationRequired - isMandatoryFingerPrintVerificationRequired
 * @param currentViewType [ViewType]
 */
data class SearchState(
    val nodes: List<MegaNode>?,
    val searchParentHandle: Long,
    val searchDrawerItem: DrawerItem? = null,
    val searchSharesTab: SharesTab = SharesTab.NONE,
    val textSubmitted: Boolean,
    val searchQuery: String?,
    val searchDepth: Int,
    val isInProgress: Boolean,
    val isMandatoryFingerPrintVerificationRequired: Boolean = false,
    val currentViewType: ViewType = ViewType.LIST,
)