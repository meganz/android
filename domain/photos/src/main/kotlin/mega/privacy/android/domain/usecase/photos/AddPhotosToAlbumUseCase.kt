package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Add photos to album use case
 */
class AddPhotosToAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: AlbumId, photoIds: List<NodeId>): Int {
        return albumRepository.addBulkPhotosToAlbum(albumId, photoIds)
    }
}
