package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Default remove albums use case implementation.
 */
class DefaultRemoveAlbums @Inject constructor(
    private val albumRepository: AlbumRepository,
) : RemoveAlbums {
    override suspend fun invoke(albumIds: List<AlbumId>) =
        albumRepository.removeAlbums(albumIds)
}
