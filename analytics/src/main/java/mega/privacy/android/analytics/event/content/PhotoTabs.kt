package mega.privacy.android.analytics.event.content

import mega.privacy.android.analytics.event.TabSelected

/**
 * TimelineTabSelected
 */
object TimelineTabSelected: TabSelected{
    override val screenView = PhotosScreenView
    override val name = "tab_timeline"
    override val uniqueIdentifier = 200
}

/**
 * AlbumsTabSelected
 */
object AlbumsTabSelected: TabSelected{
    override val screenView = PhotosScreenView
    override val name = "tab_albums"
    override val uniqueIdentifier = 201
}