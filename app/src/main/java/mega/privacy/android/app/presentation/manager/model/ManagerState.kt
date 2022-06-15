package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.app.main.DrawerItem

/**
 * Manager UI state
 *
 * @param searchDrawerItem current drawer item set for the search
 * @param searchSharedTab current shared tab set for the search
 * @param isFirstNavigationLevel true if the navigation level is the first level
 */
data class ManagerState(
    val searchDrawerItem: DrawerItem?,
    val searchSharedTab: Int,
    val isFirstNavigationLevel: Boolean
)