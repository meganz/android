package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get album photos use case.
 */
@Deprecated(message = "In favor of mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase")
fun interface GetAlbumPhotos {
    /**
     * Get album photos from album id.
     * @return a flow list of photos.
     */
    operator fun invoke(albumId: AlbumId): Flow<List<Photo>>
}
