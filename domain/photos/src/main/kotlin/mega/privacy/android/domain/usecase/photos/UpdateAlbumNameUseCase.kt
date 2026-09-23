package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Update album name use case
 *
 * @param albumRepository
 */
class UpdateAlbumNameUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: AlbumId, newName: String): String =
        albumRepository.updateAlbumName(albumId = albumId, newName = newName)

}