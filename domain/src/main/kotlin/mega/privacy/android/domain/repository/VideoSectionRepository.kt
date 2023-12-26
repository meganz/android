package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedVideoNode

/**
 * Repository related to video section
 */
interface VideoSectionRepository {
    /**
     * Get all videos
     *
     * @param order the list order
     * @return typed video node list
     */
    suspend fun getAllVideos(order: SortOrder): List<TypedVideoNode>
}