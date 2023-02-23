package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Mapper that converts a specific [String] from MegaPreferences into [UploadOption]
 */
internal fun interface UploadOptionMapper {

    /**
     * Invocation function
     *
     * @param state The current upload option from the database
     *
     * @return the corresponding [UploadOption]
     */
    operator fun invoke(state: String?): UploadOption
}