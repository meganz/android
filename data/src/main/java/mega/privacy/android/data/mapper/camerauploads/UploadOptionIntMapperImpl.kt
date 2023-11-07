package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import javax.inject.Inject

/**
 * Default implementation of [UploadOptionIntMapper]
 */
internal class UploadOptionIntMapperImpl @Inject constructor() : UploadOptionIntMapper {
    override fun invoke(option: UploadOption) = when (option) {
        UploadOption.PHOTOS -> 1001
        UploadOption.VIDEOS -> 1002
        UploadOption.PHOTOS_AND_VIDEOS -> 1003
    }
}
