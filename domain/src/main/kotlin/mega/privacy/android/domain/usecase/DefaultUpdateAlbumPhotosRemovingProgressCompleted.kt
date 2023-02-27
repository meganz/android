package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of update to acknowledge album photos removing progress is completed use case
 */
class DefaultUpdateAlbumPhotosRemovingProgressCompleted @Inject constructor(
    private val albumRepository: AlbumRepository,
) : UpdateAlbumPhotosRemovingProgressCompleted {
    override suspend fun invoke(albumId: AlbumId) {
        albumRepository.updateAlbumPhotosRemovingProgressCompleted(albumId)
    }
}
