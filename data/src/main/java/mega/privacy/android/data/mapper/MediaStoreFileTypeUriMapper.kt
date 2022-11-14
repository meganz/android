package mega.privacy.android.data.mapper

import android.net.Uri
import android.provider.MediaStore
import mega.privacy.android.domain.entity.MediaStoreFileType


/**
 * Mapper to convert [MediaStoreFileType] to related android uri
 */
typealias MediaStoreFileTypeUriMapper = (@JvmSuppressWildcards MediaStoreFileType) -> @JvmSuppressWildcards Uri

/**
 * Map [MediaStoreFileType] to [Uri]
 */
internal fun toMediaStoreFileTypeUri(mediaStoreFileType: MediaStoreFileType): Uri =
    when (mediaStoreFileType) {
        MediaStoreFileType.IMAGES_INTERNAL -> MediaStore.Images.Media.INTERNAL_CONTENT_URI
        MediaStoreFileType.IMAGES_EXTERNAL -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaStoreFileType.VIDEO_INTERNAL -> MediaStore.Video.Media.INTERNAL_CONTENT_URI
        MediaStoreFileType.VIDEO_EXTERNAL -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
