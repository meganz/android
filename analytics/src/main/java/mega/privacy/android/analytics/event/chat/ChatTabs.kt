package mega.privacy.android.analytics.event.chat

import mega.privacy.android.analytics.event.TabInfo

/**
 * ChatsTab
 */
object ChatsTabInfo : TabInfo {
    override val screenInfo = ChatScreenInfo
    override val uniqueIdentifier = 300
    override val name = "tab_chats"
}

/**
 * MeetingsTab
 */
object MeetingsTabInfo : TabInfo {
    override val screenInfo = ChatScreenInfo
    override val uniqueIdentifier = 301
    override val name = "tab_meetings"
}