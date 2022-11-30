package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Remove albums use case
 */
fun interface RemoveAlbums {
    /**
     * Remove albums based on ids.
     * @param albumIds to be removed
     */
    suspend operator fun invoke(albumIds: List<AlbumId>)
}