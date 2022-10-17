package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import javax.inject.Inject

/**
 * Filter GIF Images
 */
class DefaultFilterGIF @Inject constructor() : FilterGIF {

    override fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is GifFileTypeInfo
    }
}