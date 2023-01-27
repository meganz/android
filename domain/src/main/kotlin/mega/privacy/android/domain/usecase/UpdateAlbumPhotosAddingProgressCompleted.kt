package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Update to acknowledge album photos adding progress is completed use case
 */
fun interface UpdateAlbumPhotosAddingProgressCompleted {
    suspend operator fun invoke(albumId: AlbumId)
}
