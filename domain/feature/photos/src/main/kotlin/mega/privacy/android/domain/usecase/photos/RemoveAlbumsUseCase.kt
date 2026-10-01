package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Default remove albums use case implementation.
 */
class RemoveAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumIds: List<AlbumId>) =
        albumRepository.removeAlbums(albumIds)
}
