package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * Filter RAW Images
 */
class FilterRAWUseCase @Inject constructor() {
    /**
     * Filter RAW Images
     *
     * @return is RAW
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is RawFileTypeInfo
    }
}