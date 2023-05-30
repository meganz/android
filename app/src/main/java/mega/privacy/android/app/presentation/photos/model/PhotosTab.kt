package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.analytics.event.TabSelected
import mega.privacy.android.analytics.event.content.AlbumsTabSelected
import mega.privacy.android.analytics.event.content.TimelineTabSelected

/**
 * Photos tab
 *
 * @property analyticsInfo
 */
enum class PhotosTab(val analyticsInfo: TabSelected) {
    /**
     * Timeline
     */
    Timeline(TimelineTabSelected),

    /**
     * Albums
     */
    Albums(AlbumsTabSelected)
}