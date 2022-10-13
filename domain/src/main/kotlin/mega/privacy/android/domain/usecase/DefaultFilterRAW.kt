package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * Filter RAW Images
 */
class DefaultFilterRAW @Inject constructor(): FilterRAW {

    override fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is RawFileTypeInfo
    }
}