package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Filter RAW Images
 */
interface FilterRAW {
    /**
     * Filter RAW Images
     *
     * @return is RAW
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean
}