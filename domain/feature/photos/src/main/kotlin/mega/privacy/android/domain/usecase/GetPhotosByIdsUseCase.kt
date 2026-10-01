package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get photos by ids use case
 */
class GetPhotosByIdsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Get Photos by ids use case
     * @return a list of photos
     */
    suspend operator fun invoke(ids: List<NodeId>): List<Photo> =
        photosRepository.getPhotosByIds(ids)
}