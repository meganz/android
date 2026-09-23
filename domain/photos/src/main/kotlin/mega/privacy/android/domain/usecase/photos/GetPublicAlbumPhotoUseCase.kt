package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Get public album photo use case
 */
class GetPublicAlbumPhotoUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumPhotoIds: List<AlbumPhotoId>): List<Photo> {
        return albumRepository.getPublicPhotos(albumPhotoIds)
    }
}
