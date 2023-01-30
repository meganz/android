package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Update album name use case
 */
fun interface UpdateAlbumNameUseCase {
    /**
     * Update album name
     *
     * @param albumId the target album
     * @param newName new album name
     * @return new album name if update successfully
     */
    suspend operator fun invoke(albumId: AlbumId, newName: String): String
}