package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of update to acknowledge album photos adding progress is completed use case
 */
class DefaultUpdateAlbumPhotosAddingProgressCompleted @Inject constructor(
    private val albumRepository: AlbumRepository,
) : UpdateAlbumPhotosAddingProgressCompleted {
    override suspend fun invoke(albumId: AlbumId) {
        albumRepository.updateAlbumPhotosAddingProgressCompleted(albumId)
    }
}
