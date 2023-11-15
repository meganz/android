package mega.privacy.android.app.presentation.videosection.model

/**
 * Video section tab state
 *
 * @property tabs the tab list
 * @property selectedTab current selected tab
 */
data class VideoSectionTabState(
    val tabs: List<VideoSectionTab> = VideoSectionTab.values().asList(),
    val selectedTab: VideoSectionTab = VideoSectionTab.All,
)
