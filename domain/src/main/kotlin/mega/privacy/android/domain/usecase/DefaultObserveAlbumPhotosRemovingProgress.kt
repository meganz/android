package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of observe album photos removing progress use case
 */
class DefaultObserveAlbumPhotosRemovingProgress @Inject constructor(
    private val albumRepository: AlbumRepository,
) : ObserveAlbumPhotosRemovingProgress {
    override fun invoke(albumId: AlbumId): Flow<AlbumPhotosRemovingProgress?> {
        return albumRepository.observeAlbumPhotosRemovingProgress(albumId)
    }
}
