package mega.privacy.android.domain.usecase.impl

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.RemovePhotosFromAlbumUseCase
import javax.inject.Inject

/**
 * Implementation for the RemovePhotosFromAlbumUseCase
 */
class DefaultRemovePhotosFromAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) : RemovePhotosFromAlbumUseCase {
    override suspend fun invoke(albumId: AlbumId, photoIds: List<AlbumPhotoId>) {
        albumRepository.removePhotosFromAlbum(albumId, photoIds)
    }
}