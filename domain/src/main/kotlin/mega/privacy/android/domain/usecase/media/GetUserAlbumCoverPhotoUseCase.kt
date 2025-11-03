package mega.privacy.android.domain.usecase.media

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class GetUserAlbumCoverPhotoUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(
        albumId: AlbumId,
        selectedCoverId: Long? = null,
        refresh: Boolean = false,
    ): Photo? {
        val albumPhotos = albumRepository.getAlbumElementIDs(albumId = albumId, refresh = refresh)
        if (albumPhotos.isEmpty()) return null

        val cover = selectedCoverId
            ?.let { coverId -> albumPhotos.find { it.id == coverId } }
            ?: albumPhotos.lastOrNull()
            ?: return null

        return photosRepository.getPhotoFromNodeID(
            nodeId = cover.nodeId,
            albumPhotoId = cover,
            refresh = refresh,
        )
    }
}