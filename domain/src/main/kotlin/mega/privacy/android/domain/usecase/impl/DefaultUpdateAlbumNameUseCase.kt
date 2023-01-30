package mega.privacy.android.domain.usecase.impl

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.UpdateAlbumNameUseCase
import javax.inject.Inject

/**
 * Update album name use case
 *
 * @param albumRepository
 */
class DefaultUpdateAlbumNameUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) : UpdateAlbumNameUseCase {

    override suspend fun invoke(albumId: AlbumId, newName: String): String =
        albumRepository.updateAlbumName(albumId = albumId, newName = newName)

}