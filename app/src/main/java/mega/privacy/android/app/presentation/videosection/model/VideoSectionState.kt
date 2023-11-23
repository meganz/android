package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.SortOrder

/**
 * The state is for the videos section
 *
 * @property allVideos the all video items
 * @property sortOrder the sort order of video items
 * @property isPendingRefresh
 */
data class VideoSectionState(
    val allVideos: List<UIVideo> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false
)
