package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.analytics.event.TabInfo
import mega.privacy.android.analytics.event.content.AlbumsTabInfo
import mega.privacy.android.analytics.event.content.TimelineTabInfo

/**
 * Photos tab
 *
 * @property analyticsInfo
 */
enum class PhotosTab(val analyticsInfo: TabInfo) {
    /**
     * Timeline
     */
    Timeline(TimelineTabInfo),

    /**
     * Albums
     */
    Albums(AlbumsTabInfo)
}