package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Filter RAW Images
 */
class DefaultFilterRAW : FilterRAW {

    override fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is RawFileTypeInfo
    }
}