package mega.privacy.android.analytics.event.file

import mega.privacy.android.analytics.event.TabInfo


/**
 * IncomingSharesTabInfo
 */
object IncomingSharesTabInfo : TabInfo {
    override val screenInfo = SharedItemsScreenInfo
    override val name = "tab_incoming_shares"
    override val uniqueIdentifier = 400
}


/**
 * OutgoingSharesTabInfo
 */
object OutgoingSharesTabInfo : TabInfo {
    override val screenInfo = SharedItemsScreenInfo
    override val name = "tab_outgoing_shares"
    override val uniqueIdentifier = 401
}

/**
 * LinkSharesTabInfo
 */
object LinkSharesTabInfo : TabInfo {
    override val screenInfo = SharedItemsScreenInfo
    override val name = "tab_link_shares"
    override val uniqueIdentifier = 402
}