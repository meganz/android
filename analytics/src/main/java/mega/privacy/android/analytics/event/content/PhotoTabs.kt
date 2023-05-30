package mega.privacy.android.analytics.event.content

import mega.privacy.android.analytics.event.TabInfo

/**
 * TimelineTabSelected
 */
object TimelineTabInfo : TabInfo {
    override val screenInfo = PhotosScreenInfo
    override val name = "tab_timeline"
    override val uniqueIdentifier = 200
}

/**
 * AlbumsTabSelected
 */
object AlbumsTabInfo : TabInfo {
    override val screenInfo = PhotosScreenInfo
    override val name = "tab_albums"
    override val uniqueIdentifier = 201
}