package mega.privacy.android.domain.usecase

import AlbumEntity
import mega.privacy.android.domain.entity.photos.PhotoPredicate

/**
 * Get default albums map
 */
fun interface GetDefaultAlbumsMap {
    operator fun invoke(): Map<AlbumEntity, PhotoPredicate>
}