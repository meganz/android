package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.VideoNode

/**
 * Repository related to videos
 */
interface VideoSectionRepository {
    /**
     * Get all videos
     *
     * @param order the list order
     * @return video node list
     */
    suspend fun getAllVideos(order: SortOrder): List<VideoNode>
}