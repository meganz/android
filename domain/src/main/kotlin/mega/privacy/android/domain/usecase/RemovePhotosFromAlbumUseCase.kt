package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId

/**
 * Remove photos from an album use case
 */
fun interface RemovePhotosFromAlbumUseCase {
    /**
     * Remove photos from album
     *
     * @param albumId is the target album
     * @param photoIds is the list of photo ids to be removed from album
     */
    suspend operator fun invoke(albumId: AlbumId, photoIds: List<AlbumPhotoId>)
}