package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Default add photos to album use case implementation
 */
class DefaultAddPhotosToAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
) : AddPhotosToAlbum {
    override suspend fun invoke(albumId: AlbumId, photoIds: List<NodeId>) {
        albumRepository.addPhotosToAlbum(albumId, photoIds)
    }
}
