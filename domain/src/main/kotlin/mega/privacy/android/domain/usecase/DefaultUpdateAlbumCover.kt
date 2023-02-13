package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of update album cover use case
 */
class DefaultUpdateAlbumCover @Inject constructor(
    private val albumRepository: AlbumRepository,
) : UpdateAlbumCover {
    override suspend fun invoke(albumId: AlbumId, elementId: NodeId) =
        albumRepository.updateAlbumCover(albumId, elementId)
}
