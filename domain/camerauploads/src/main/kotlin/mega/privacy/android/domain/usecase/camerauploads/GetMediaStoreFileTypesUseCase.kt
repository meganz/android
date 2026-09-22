package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import javax.inject.Inject

/**
 * Use case for retrieving list of [MediaStoreFileType]
 *
 * @property getUploadOptionUseCase [GetUploadOptionUseCase]
 */
class GetMediaStoreFileTypesUseCase @Inject constructor(
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
) {

    /**
     * invoke
     * @return list of [MediaStoreFileType]
     */
    suspend operator fun invoke(): List<MediaStoreFileType> {
        val mediaStoreFileTypes = mutableListOf<MediaStoreFileType>()
        when (getUploadOptionUseCase()) {
            UploadOption.PHOTOS -> {
                mediaStoreFileTypes.add(MediaStoreFileType.IMAGES_INTERNAL)
                mediaStoreFileTypes.add(MediaStoreFileType.IMAGES_EXTERNAL)
            }

            UploadOption.VIDEOS -> {
                mediaStoreFileTypes.add(MediaStoreFileType.VIDEO_INTERNAL)
                mediaStoreFileTypes.add(MediaStoreFileType.VIDEO_EXTERNAL)
            }

            UploadOption.PHOTOS_AND_VIDEOS -> {
                mediaStoreFileTypes.add(MediaStoreFileType.IMAGES_INTERNAL)
                mediaStoreFileTypes.add(MediaStoreFileType.IMAGES_EXTERNAL)
                mediaStoreFileTypes.add(MediaStoreFileType.VIDEO_INTERNAL)
                mediaStoreFileTypes.add(MediaStoreFileType.VIDEO_EXTERNAL)
            }
        }
        return mediaStoreFileTypes
    }
}
