package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get album photos use case.
 */
fun interface GetAlbumPhotos {
    /**
     * Get album photos from album id.
     * @return a flow list of photos.
     */
    suspend operator fun invoke(albumId: AlbumId): Flow<List<Photo>>
}
