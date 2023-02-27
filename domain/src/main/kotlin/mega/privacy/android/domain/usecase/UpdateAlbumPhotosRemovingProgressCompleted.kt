package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Update to acknowledge album photos removing progress is completed use case
 */
fun interface UpdateAlbumPhotosRemovingProgressCompleted {
    suspend operator fun invoke(albumId: AlbumId)
}
