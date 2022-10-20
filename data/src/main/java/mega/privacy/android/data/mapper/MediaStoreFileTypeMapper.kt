package mega.privacy.android.data.mapper

import android.net.Uri
import android.provider.MediaStore
import mega.privacy.android.domain.entity.MediaStoreFileType


/**
 * Mapper to convert android uri to [MediaStoreFileType]
 */
typealias MediaStoreFileTypeMapper = (@JvmSuppressWildcards Uri?) -> @JvmSuppressWildcards MediaStoreFileType?

/**
 * Map [Uri] to [MediaStoreFileType]
 */
internal fun toMediaStoreFileType(uri: Uri?): MediaStoreFileType? =
    when (uri) {
        MediaStore.Images.Media.INTERNAL_CONTENT_URI -> MediaStoreFileType.IMAGES_INTERNAL
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI -> MediaStoreFileType.IMAGES_EXTERNAL
        MediaStore.Video.Media.INTERNAL_CONTENT_URI -> MediaStoreFileType.VIDEO_INTERNAL
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI -> MediaStoreFileType.VIDEO_EXTERNAL
        else -> null
    }
