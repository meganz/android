package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class AlbumHasSensitiveContentUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(albumId: AlbumId, refresh: Boolean = false) =
        albumRepository
            .getAlbumElementIDs(albumId, refresh)
            .any { albumPhotoId ->
                val photo = photosRepository.getPhotoFromNodeID(
                    nodeId = albumPhotoId.nodeId,
                    albumPhotoId = albumPhotoId,
                    refresh = refresh,
                )
                photo != null && (photo.isSensitive || photo.isSensitiveInherited)
            }
}