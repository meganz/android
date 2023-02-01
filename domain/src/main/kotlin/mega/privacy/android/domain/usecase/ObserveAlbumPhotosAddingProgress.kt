package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress

/**
 * Observe album photos adding progress use case
 */
fun interface ObserveAlbumPhotosAddingProgress {
    operator fun invoke(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?>
}
