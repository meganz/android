package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotos
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Get public album use case
 */
class GetPublicAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumLink: AlbumLink): AlbumPhotos {
        val (userSet, photoIds) = albumRepository.fetchPublicAlbum(albumLink)

        val album = Album.UserAlbum(
            id = AlbumId(userSet.id),
            title = userSet.name,
            cover = null,
            creationTime = userSet.creationTime,
            modificationTime = userSet.modificationTime,
            isExported = userSet.isExported,
        )
        val photos = albumRepository.getPublicPhotos(photoIds)

        return album to photos
    }
}
