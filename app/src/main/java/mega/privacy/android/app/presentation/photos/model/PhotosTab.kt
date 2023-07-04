package mega.privacy.android.app.presentation.photos.model

import mega.privacy.mobile.analytics.core.event.identifier.TabSelectedEventIdentifier
import mega.privacy.mobile.analytics.event.AlbumsTabEvent
import mega.privacy.mobile.analytics.event.TimelineTabEvent


/**
 * Photos tab
 *
 * @property analyticsInfo
 */
enum class PhotosTab(val analyticsInfo: TabSelectedEventIdentifier) {
    /**
     * Timeline
     */
    Timeline(TimelineTabEvent),

    /**
     * Albums
     */
    Albums(AlbumsTabEvent)
}