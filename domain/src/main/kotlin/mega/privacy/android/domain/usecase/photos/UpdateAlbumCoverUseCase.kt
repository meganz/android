package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Update album cover use case
 */
class UpdateAlbumCoverUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: AlbumId, elementId: NodeId) =
        albumRepository.updateAlbumCover(albumId, elementId)
}
