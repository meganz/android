package mega.privacy.android.app.presentation.node

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject

/**
 * Node source mapper
 *
 * Creates the search type based on which location search is initiated.
 */
class NodeSourceTypeMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param drawerItem the selected drawer item from manager activity
     * @param sharesTab selected shares tab
     */
    operator fun invoke(drawerItem: DrawerItem?, sharesTab: SharesTab?) = when (drawerItem) {
        DrawerItem.HOMEPAGE, DrawerItem.CLOUD_DRIVE -> NodeSourceType.CLOUD_DRIVE
        DrawerItem.BACKUPS -> NodeSourceType.BACKUPS
        DrawerItem.RUBBISH_BIN -> NodeSourceType.RUBBISH_BIN
        DrawerItem.SHARED_ITEMS -> when (sharesTab) {
            SharesTab.INCOMING_TAB -> NodeSourceType.INCOMING_SHARES
            SharesTab.OUTGOING_TAB -> NodeSourceType.OUTGOING_SHARES
            SharesTab.LINKS_TAB -> NodeSourceType.LINKS
            else -> NodeSourceType.OTHER
        }

        else -> NodeSourceType.OTHER
    }
}