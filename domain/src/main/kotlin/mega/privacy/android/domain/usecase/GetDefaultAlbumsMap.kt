package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.AlbumEntity
import mega.privacy.android.domain.entity.photos.PhotoPredicate

/**
 * Get default albums map
 */
fun interface GetDefaultAlbumsMap {
    /**
     * Get default albums map
     */
    operator fun invoke(): Map<AlbumEntity, PhotoPredicate>
}