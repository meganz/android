package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * Filter Favourite photos
 */
class FilterFavouriteUseCase @Inject constructor() {
    /**
     * Filter Favourite photos
     *
     * @return is favourite
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean =
        {
            it.isFavourite && (it is Photo.Video || it is Photo.Image)
        }
}
