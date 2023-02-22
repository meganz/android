package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import javax.inject.Inject

/**
 * Default implementation of [UploadOptionIntMapper]
 */
class UploadOptionIntMapperImpl @Inject constructor() : UploadOptionIntMapper {
    override fun invoke(option: UploadOption) = when (option) {
        UploadOption.PHOTOS -> MegaPreferences.ONLY_PHOTOS
        UploadOption.VIDEOS -> MegaPreferences.ONLY_VIDEOS
        UploadOption.PHOTOS_AND_VIDEOS -> MegaPreferences.PHOTOS_AND_VIDEOS
    }
}