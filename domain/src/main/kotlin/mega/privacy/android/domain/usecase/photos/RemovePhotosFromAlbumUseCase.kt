package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * UseCase for Removing Photo items from an Album
 */
class RemovePhotosFromAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: AlbumId, photoIds: List<AlbumPhotoId>) {
        albumRepository.removePhotosFromAlbum(albumId, photoIds)
    }
}