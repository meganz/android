package mega.privacy.android.feature.photos.model

import mega.privacy.mobile.analytics.core.event.identifier.TabSelectedEventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenAlbumsTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenPlaylistsTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenTimelineTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenVideosTabEvent

/**
 * The list of screens in Media feature
 *
 * @property analyticsInfo The analytics event identifier for tab selection
 */
enum class MediaScreen(val analyticsInfo: TabSelectedEventIdentifier) {
    /**
     * Timeline Tab
     */
    Timeline(MediaScreenTimelineTabEvent),

    /**
     * Albums Tab
     */
    Albums(MediaScreenAlbumsTabEvent),

    /**
     * Videos Tab
     */
    Videos(MediaScreenVideosTabEvent),

    /**
     * Playlists Tab
     */
    Playlists(MediaScreenPlaylistsTabEvent)
}
