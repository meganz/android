package mega.privacy.android.app.presentation.videosection.model

/**
 * The state is for the videos section
 *
 * @property allVideos the all video items
 */
data class VideoSectionState(
    val allVideos: List<UIVideo> = emptyList()
)
