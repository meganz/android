package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Filter Favourite photos
 */
interface FilterFavourite {
    /**
     * Filter Favourite photos
     *
     * @return is favourite
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean
}