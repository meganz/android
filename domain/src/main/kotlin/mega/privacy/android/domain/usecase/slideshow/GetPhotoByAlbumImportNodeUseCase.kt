package mega.privacy.android.domain.usecase.slideshow

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Get photo by album import node
 */
class GetPhotoByAlbumImportNodeUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    /**
     * Get Photo by album import node
     *
     * @return photo
     */
    suspend operator fun invoke(nodeId: NodeId): Photo? = albumRepository.getPublicPhoto(nodeId)
}
