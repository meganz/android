package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress

/**
 * Observe album photos removing progress use case
 */
fun interface ObserveAlbumPhotosRemovingProgress {
    operator fun invoke(albumId: AlbumId): Flow<AlbumPhotosRemovingProgress?>
}
