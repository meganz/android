package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Filter GIF Images
 */
interface FilterGIF {
    /**
     * Filter GIF Images
     *
     * @return is GIF
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean
}