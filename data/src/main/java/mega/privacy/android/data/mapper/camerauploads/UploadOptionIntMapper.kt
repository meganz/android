package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Mapper that converts [UploadOption] into related MegaPreferences values for selecting
 * the Upload Option of Camera Uploads
 */
internal fun interface UploadOptionIntMapper {

    /**
     * Invocation function
     *
     * @param option [UploadOption]
     *
     * @return The corresponding MegaPreferences value
     */
    operator fun invoke(option: UploadOption): Int
}