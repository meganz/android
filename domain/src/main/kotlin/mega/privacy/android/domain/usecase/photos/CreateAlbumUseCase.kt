package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use Case to create an album
 */
class CreateAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(name: String): Album.UserAlbum {
        val newUserSet = albumRepository.createAlbum(name)
        val coverPhoto = null
        return Album.UserAlbum(
            id = AlbumId(newUserSet.id),
            title = newUserSet.name,
            cover = coverPhoto,
            modificationTime = newUserSet.modificationTime,
            isExported = newUserSet.isExported,
        )
    }
}