package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.PhotoPredicate

/**
 * Get default albums map
 */
fun interface GetDefaultAlbumsMap {
    /**
     * Get default albums map
     */
    operator fun invoke(): Map<Album, PhotoPredicate>
}