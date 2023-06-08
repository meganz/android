package mega.privacy.android.analytics.event.file

import mega.privacy.android.analytics.event.TabInfo


/**
 * RecentsTab
 */
object RecentsTabInfo : TabInfo {
    override val screenInfo = HomeScreenInfo
    override val name = "tab_recents"
    override val uniqueIdentifier = 0
}


/**
 * OfflineTab
 */
object OfflineTabInfo : TabInfo {
    override val screenInfo = HomeScreenInfo
    override val name = "tab_offline"
    override val uniqueIdentifier = 1
}
