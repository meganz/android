package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of observe album photos adding progress use case
 */
class DefaultObserveAlbumPhotosAddingProgress @Inject constructor(
    private val albumRepository: AlbumRepository,
) : ObserveAlbumPhotosAddingProgress {
    override fun invoke(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?> {
        return albumRepository.observeAlbumPhotosAddingProgress(albumId)
    }
}
