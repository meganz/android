package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get photos by ids use case
 */
fun interface GetPhotosByIds {
    /**
     * Get Photos by ids use case
     * @return a list of photos
     */
    suspend operator fun invoke(ids: List<NodeId>): List<Photo>
}