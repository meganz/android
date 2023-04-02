package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * Filter GIF Images
 */
class FilterGIFUseCase @Inject constructor() {
    /**
     * Filter GIF Images
     *
     * @return is GIF
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is GifFileTypeInfo
    }
}