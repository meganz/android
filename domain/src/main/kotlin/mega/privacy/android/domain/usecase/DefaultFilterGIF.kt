package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Filter GIF Images
 */
class DefaultFilterGIF : FilterGIF {

    override fun invoke(): suspend (photo: Photo) -> Boolean = {
        it.fileTypeInfo is GifFileTypeInfo
    }
}