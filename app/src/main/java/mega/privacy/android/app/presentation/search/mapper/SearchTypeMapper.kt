package mega.privacy.android.app.presentation.search.mapper

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.entity.search.SearchType
import javax.inject.Inject

/**
 * Search type mapper
 *
 * Creates the search type based on which location search is initiated.
 */
class SearchTypeMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param drawerItem the selected drawer item from manager activity
     * @param sharesTab selected shares tab
     */
    operator fun invoke(drawerItem: DrawerItem?, sharesTab: SharesTab?) = when (drawerItem) {
        DrawerItem.HOMEPAGE, DrawerItem.CLOUD_DRIVE -> SearchType.CLOUD_DRIVE
        DrawerItem.BACKUPS -> SearchType.BACKUPS
        DrawerItem.RUBBISH_BIN -> SearchType.RUBBISH_BIN
        DrawerItem.SHARED_ITEMS -> when (sharesTab) {
            SharesTab.INCOMING_TAB -> SearchType.INCOMING_SHARES
            SharesTab.OUTGOING_TAB -> SearchType.OUTGOING_SHARES
            SharesTab.LINKS_TAB -> SearchType.LINKS
            else -> SearchType.OTHER
        }

        else -> SearchType.OTHER
    }
}